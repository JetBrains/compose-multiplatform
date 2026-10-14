/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi::class)

package org.jetbrains.compose.web

import org.jetbrains.compose.web.dom.appendEscapedAttribute
import org.jetbrains.compose.web.internal.runtime.SuspenseMarker

private fun script(options: AsyncHtmlRenderOptions, code: String): String = buildString {
    append("<script data-compose-stream-script=\"").append(options.identifierPrefix).append('"')
    options.nonce?.let {
        append(" nonce=\"")
        appendEscapedAttribute(it)
        append('"')
    }
    append(">(function(){var current=document.currentScript;if(current)current.remove();")
    append(code).append("})();</script>")
}

// Parse in the parent's context to preserve table and SVG markup. Keep replacement scripts inert
// when moving them into the document.
internal fun streamBootstrap(options: AsyncHtmlRenderOptions): String = script(options,
    """var key=${jsString(options.identifierPrefix)},startMarker=${jsString("$SuspenseMarker:")},endMarker=${jsString("/$SuspenseMarker:")};
var streams=globalThis.__composeHtmlStreams||(globalThis.__composeHtmlStreams=Object.create(null));
if(streams[key])throw new Error("Duplicate Compose HTML stream");
var ranges=Object.create(null);
function index(root){
var w=document.createTreeWalker(root,128),n;
while(n=w.nextNode()){
if(n.data.indexOf(startMarker+key+"_")===0){var id=n.data.slice(startMarker.length);(ranges[id]||(ranges[id]={})).start=n;}
else if(n.data.indexOf(endMarker+key+"_")===0){var id=n.data.slice(endMarker.length);(ranges[id]||(ranges[id]={})).end=n;}
}}
index(document);
streams[key]={replace:function(id,html){
var entry=ranges[id];delete ranges[id];
if(!entry)return;
var s=entry.start,e=entry.end;
if(!s||!e||!s.isConnected||!e.isConnected)return;
var r=document.createRange();r.setStartAfter(s);r.setEndBefore(e);
var f=r.createContextualFragment(html);
var scripts=f.querySelectorAll("script"),holder=document.createElement("div");
for(var i=0;i<scripts.length;i++){
var script=scripts[i],svg=script.namespaceURI==="http://www.w3.org/2000/svg";
if(!svg&&script.namespaceURI!=="http://www.w3.org/1999/xhtml")continue;
holder.innerHTML=svg?"<svg>"+script.outerHTML+"</svg>":script.outerHTML;
var inert=svg?holder.firstChild.firstChild:holder.firstChild;
script.parentNode.replaceChild(inert,script);
}
index(f);
r.deleteContents();e.parentNode.insertBefore(f,e);${if (options.hydratable) "s.data=${jsString(SuspenseMarker)};e.data=${jsString("/$SuspenseMarker")};" else "s.remove();e.remove();"}
},complete:function(states){
var all=document.querySelectorAll("[$HydrationStateAttribute][$StreamPendingAttribute]");
for(var i=0;i<states.length;i++){for(var j=0;j<all.length;j++){
if(all[j].getAttribute("$StreamPendingAttribute")===key&&all[j].getAttribute("$HydrationForAttribute")===states[i][0])all[j].textContent=states[i][1];}}
var roots=document.querySelectorAll("[$StreamPendingAttribute]");
for(var i=0;i<roots.length;i++)if(roots[i].getAttribute("$StreamPendingAttribute")===key)roots[i].removeAttribute("$StreamPendingAttribute");
delete streams[key];document.dispatchEvent(new CustomEvent("compose-stream-complete",{detail:{identifierPrefix:key}}));
},fail:function(){
var roots=document.querySelectorAll("[$StreamPendingAttribute]");
for(var i=0;i<roots.length;i++)if(roots[i].getAttribute("$StreamPendingAttribute")===key)roots[i].setAttribute("$StreamErrorAttribute",key);
delete streams[key];document.dispatchEvent(new CustomEvent("compose-stream-error",{detail:{identifierPrefix:key}}));
}};""")

internal fun streamReplacement(options: AsyncHtmlRenderOptions, id: String, html: String): String =
    script(options, "globalThis.__composeHtmlStreams[${jsString(options.identifierPrefix)}].replace(${jsString(id)},${jsString(html)});")

internal fun streamCompletion(options: AsyncHtmlRenderOptions, states: List<Pair<String?, String>>): String {
    val payload = states.joinToString(",", prefix = "[", postfix = "]") { (id, state) ->
        "[" + (id?.let(::jsString) ?: "null") + "," + jsString(state) + "]"
    }
    return script(options, "globalThis.__composeHtmlStreams[${jsString(options.identifierPrefix)}].complete($payload);")
}

internal fun streamFailure(options: AsyncHtmlRenderOptions): String = script(options,
    "var streams=globalThis.__composeHtmlStreams,key=${jsString(options.identifierPrefix)};if(streams&&streams[key])streams[key].fail();")

/** Escapes a JS string for use inside an HTML script element. */
internal fun jsString(value: String): String = buildString {
    append('"')
    for (character in value) {
        when {
            character == '"' -> append("\\\"")
            character == '\\' -> append("\\\\")
            character < ' ' || character in "<>&\u2028\u2029" -> {
                append("\\u").append(character.code.toString(16).padStart(4, '0'))
            }
            else -> append(character)
        }
    }
    append('"')
}
