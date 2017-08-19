package com.google.r4a;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import kotlin.Function;
import kotlin.collections.AbstractMutableMap;
import kotlin.jvm.functions.Function0;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class R4a {

    private static final Object NO_VALUE_SPECIFIED = new Object();

    public static void renderIntoWrapperView(final List<Element> oldElements, final List<Element> newElements, final ViewGroup container, final int startIndex, final int endIndex) {
        Looper mainLooper = Looper.getMainLooper();
        boolean isUiThread = Thread.currentThread() == mainLooper.getThread();
        if(isUiThread) renderIntoWrapperView_MainThread(oldElements, newElements, container, startIndex, endIndex);
        else {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    renderIntoWrapperView_MainThread(oldElements, newElements, container, startIndex, endIndex);
                }
            });
        }
    }

    private static void renderIntoWrapperView_MainThread(List<Element> oldElements, List<Element> newElements, ViewGroup container, int startIndex, int endIndex)
    {
        if (oldElements == null) oldElements = Collections.emptyList();
        if (endIndex - startIndex < 0)
            throw new IllegalStateException("Expected start index (" + startIndex + ") to be less than or equal to end index (" + endIndex + ")");

        int position = 0;
        while (position < newElements.size()) {
            Element oldElement = position < oldElements.size() ? oldElements.get(position) : null;
            Element newElement = newElements.get(position);
            View child = startIndex + position < endIndex ? container.getChildAt(startIndex + position) : null;

            // If we can reuse the element, do so
            if (child != null && oldElement != null && child == oldElement.getChild() && Objects.equals(oldElement.getSubstitutedType(), newElement.getSubstitutedType()) && Objects.equals(oldElement.getDefaultType(), newElement.getDefaultType())) {
                newElement.setChild(child);
                updateChildAttributes(container, oldElement, newElement, startIndex + position);
                position++;
                continue;
            }

            if (child != null) container.removeViewAt(startIndex + position);
            child = initializeChild(container, newElement, startIndex + position);
            newElement.setChild(child);
            position++;
            continue;
        }

        if (startIndex + position < endIndex) {
            container.removeViews(startIndex + position, endIndex - position);
        }
    }



    private static View initializeChild(ViewGroup container, Element element, int position)
    {
        Context context = container.getContext();
        try {
            View child = createInstance(element, container.getContext());
            for(String attributeKey : element.getAttributes().keySet()) {
                // Handle radio button's checked attribute after adding it to the RadioGroup to bind the group before setting checked
                if (child instanceof RadioButton && "checked".equals(attributeKey)) continue;
                if(element.getDefaultType() != null) continue; // TODO: Initialize using data binding.
                if(attributeKey.startsWith("layout_")) continue;
                setAttribute(child, container, attributeKey, NO_VALUE_SPECIFIED, element.getAttributes().get(attributeKey));
            }
            if(!element.getAttributes().containsKey("layoutParams"))
                setLayoutParams(container, child, element.getAttributes());
            container.addView(child, position);
            if (child instanceof RadioButton && element.getAttributes().containsKey("checked")) setAttribute(child, container, "checked", NO_VALUE_SPECIFIED, element.getAttributes().get("checked"));
            return child;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void setLayoutParams(ViewGroup container, View child, Map<String, Object> attributes) {
        if(container instanceof LinearLayout) {
            Integer width = null;
            Integer height = null;
            Integer weight = null;
            for(Map.Entry<String, Object> attribute : attributes.entrySet()) {
                if(attribute.getKey().equals("layout_width")) {
                    if(attribute.getValue() instanceof Integer) width = (Integer)attribute.getValue();
                    if("match_parent".equals(attribute.getValue())) width = LinearLayout.LayoutParams.MATCH_PARENT;
                    if("wrap_content".equals(attribute.getValue())) width = LinearLayout.LayoutParams.WRAP_CONTENT;
                }
                if(attribute.getKey().equals("layout_height")) {
                    if(attribute.getValue() instanceof Integer) height = (Integer)attribute.getValue();
                    if("match_parent".equals(attribute.getValue())) height = LinearLayout.LayoutParams.MATCH_PARENT;
                    if("wrap_content".equals(attribute.getValue())) height = LinearLayout.LayoutParams.WRAP_CONTENT;
                }
                if(attribute.getKey().equals("layout_weight")) {
                    if(attribute.getValue() instanceof Integer) weight = (Integer)attribute.getValue();
                    if(attribute.getValue() instanceof String) weight = Integer.parseInt((String)attribute.getValue());
                }
            }
            if(width != null && height != null) {
                if(weight == null) child.setLayoutParams(new LinearLayout.LayoutParams(width, height));
                else child.setLayoutParams(new LinearLayout.LayoutParams(width, height, weight));
            }

        }
    }

    private static View createInstance(Element element, Context context) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException {

        if(element.getDefaultType() != null) {
            return LayoutInflater.from(context).inflate((Integer) element.getAttributes().get("layout"), null); // TODO: root should be parent view
       /*     MyIncludableLayoutBinding valueBinding = DataBindingUtil.inflate(LayoutInflater.from(context), element.attributes.get("layout"), (LinearLayout)view, false);
            valueBinding.setValue(value);
            View valueView = valueBinding.getRoot();
            return valueBinding.get
            */
        }

        Class<?> cls = element.getSubstitutedType();
        return (View)cls.getConstructor(Context.class).newInstance(context);
    }

    private static View updateChildAttributes(ViewGroup container, Element oldElement, Element newElement, int position) {
        Context context = container.getContext();
        View child = (View) container.getChildAt(position);

        if(newElement.getDefaultType() != null) return child; // TODO: Re-bind the data if this element uses data binding.

        long start = System.currentTimeMillis();
        try {
            // Find attributes that need to be removed/reset
            for (String attributeKey : oldElement.getAttributes().keySet()) {
                if (newElement.getAttributes().containsKey(attributeKey)) continue;
                setAttribute(child, container, attributeKey, oldElement.getAttributes().get(attributeKey), NO_VALUE_SPECIFIED);
            }

            // Find attributes that need to be added/set/changed
            for (String attributeKey : newElement.getAttributes().keySet()) {
                if(attributeKey.startsWith("layout_")) continue;
                Object oldAttributeValue = oldElement.getAttributes().containsKey(attributeKey) ? oldElement.getAttributes().get(attributeKey) : NO_VALUE_SPECIFIED;
                Object newAttributeValue = newElement.getAttributes().containsKey(attributeKey) ? newElement.getAttributes().get(attributeKey) : NO_VALUE_SPECIFIED;
                setAttribute(child, container, attributeKey, oldAttributeValue, newAttributeValue);
            }

            // Determine if the layout params need to be changed
            boolean layoutParamsChange = false;
            for (String attributeKey : newElement.getAttributes().keySet()) {
                if(!attributeKey.startsWith("layout_")) continue;
                Object oldAttributeValue = oldElement.getAttributes().containsKey(attributeKey) ? oldElement.getAttributes().get(attributeKey) : NO_VALUE_SPECIFIED;
                Object newAttributeValue = newElement.getAttributes().containsKey(attributeKey) ? newElement.getAttributes().get(attributeKey) : NO_VALUE_SPECIFIED;
                if(!oldAttributeValue.equals(newAttributeValue)) layoutParamsChange = true;
            }
            if(layoutParamsChange) setLayoutParams(container, child, newElement.getAttributes());

            return child;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isCoercable(Object value, Class<?> type) {
        if(type.isPrimitive() && value == null) return false;
        try { coerceValue(value, type); return true; }
        catch(IllegalArgumentException e){ return false; }
    }

    private static float coercePixels(Context context, Object value) {
        if(value instanceof Integer) return (Integer)value;
        if(value instanceof Float) return (Float)value;
        if(value instanceof String) {
            String str = (String)value;
            if(str.endsWith("px")) {
                return Integer.parseInt(str.substring(0, str.length()-"px".length()));
            }
            if(str.endsWith("in")) {
                DisplayMetrics metrics = context.getResources().getDisplayMetrics();
                return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_IN, Float.parseFloat(str.substring(0, str.length()-"in".length())), metrics) / metrics.scaledDensity;
            }
            if(str.endsWith("mm")) {
                DisplayMetrics metrics = context.getResources().getDisplayMetrics();
                return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, Float.parseFloat(str.substring(0, str.length()-"mm".length())), metrics) / metrics.scaledDensity;
            }
            if(str.endsWith("pt")) {
                DisplayMetrics metrics = context.getResources().getDisplayMetrics();
                return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PT, Float.parseFloat(str.substring(0, str.length()-"pt".length())), metrics) / metrics.scaledDensity;
            }
            if(str.endsWith("dp")) {
                DisplayMetrics metrics = context.getResources().getDisplayMetrics();
                return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, Float.parseFloat(str.substring(0, str.length()-"dp".length())), metrics) / metrics.scaledDensity;
            }
            if(str.endsWith("dip")) {
                DisplayMetrics metrics = context.getResources().getDisplayMetrics();
                return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, Float.parseFloat(str.substring(0, str.length()-"dip".length())), metrics) / metrics.scaledDensity;
            }
            if(str.endsWith("sp")) {
                DisplayMetrics metrics = context.getResources().getDisplayMetrics();
                return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, Float.parseFloat(str.substring(0, str.length()-"sp".length())), metrics) / metrics.scaledDensity;
            }
            return Integer.parseInt(str);
        }
        throw new IllegalArgumentException("Don't know how to coerce "+value+"("+value.getClass()+") into pixels (integer)");
    }

    private static <T> T coerceValue(final Object value, Class<T> cls)
    {
        if(value == null) {
            if(cls.isPrimitive()) throw new IllegalArgumentException("Attribute value is null and therefore can not be coerced into "+cls);
            else return null;
        }
        if(cls.isAssignableFrom(value.getClass())) return cls.cast(value);

        if(cls.isPrimitive()) {
            if(value instanceof Integer && Integer.TYPE.equals(cls)) return (T)value;
            if(value instanceof Integer && Float.TYPE.equals(cls)) return (T)(new Float(((Integer)value).floatValue()));
            if(value instanceof Integer && Double.TYPE.equals(cls)) return (T)(new Double(((Integer)value).doubleValue()));
            if(value instanceof Boolean && Boolean.TYPE.equals(cls)) return (T)value;
            if(value instanceof Float && Float.TYPE.equals(cls)) return (T)value;
            if(value instanceof Double && Double.TYPE.equals(cls)) return (T)value;
            if(value instanceof Short && Short.TYPE.equals(cls)) return (T)value;
            if(value instanceof Long && Long.TYPE.equals(cls)) return (T)value;
            if(value instanceof Character && Character.TYPE.equals(cls)) return (T)value;
            if(value instanceof Byte && Byte.TYPE.equals(cls)) return (T)value;
        }

        if(Boolean.class.equals(cls) && "false".equals(value)) return cls.cast(false);
        if(Boolean.class.equals(cls) && "true".equals(value)) return cls.cast(true);
        if(Float.class.equals(cls) && value instanceof Integer) { return cls.cast(new Float((int)value)); }
        if(Integer.class.equals(cls) && value instanceof String) {
            try {
                return cls.cast(Integer.parseInt((String)value));
            } catch(NumberFormatException e) { throw new IllegalArgumentException(value+" could not be coerced to Integer"); }
        }
        if(Long.class.equals(cls) && value instanceof String) {
            try {
                return cls.cast(Long.parseLong((String)value));
            } catch(NumberFormatException e) { throw new IllegalArgumentException(value+" could not be coerced to Long"); }
        }
        if(Float.class.equals(cls) && value instanceof String) {
            try {
                return cls.cast(Float.parseFloat((String)value));
            } catch(NumberFormatException e) { throw new IllegalArgumentException(value+" could not be coerced to Float"); }
        }
        if(Double.class.equals(cls) && value instanceof String) {
            try {
                return cls.cast(Double.parseDouble((String)value));
            } catch(NumberFormatException e) { throw new IllegalArgumentException(value+" could not be coerced to Double"); }
        }
        if(Byte.class.equals(cls) && value instanceof String) {
            try {
                return cls.cast(Byte.parseByte((String)value));
            } catch(NumberFormatException e) { throw new IllegalArgumentException(value+" could not be coerced to Double"); }
        }
        if(Short.class.equals(cls) && value instanceof String) {
            try {
                return cls.cast(Short.parseShort((String)value));
            } catch(NumberFormatException e) { throw new IllegalArgumentException(value+" could not be coerced to Short"); }
        }
        if(Character.class.equals(cls) && value instanceof String) {
            String str = (String) value;
            if(str.length() == 1) return cls.cast(str.charAt(0));
            throw new IllegalArgumentException("Characters must be of length 1 ('"+str+"' is of length "+str.length()+")");
        }

        // TODO: We need to un-hard-code this, generalize it to all single-function interfaces, somehow
        if(View.OnClickListener.class.equals(cls) && value instanceof Function0)
        {
            return cls.cast((View.OnClickListener)new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ((Function0)value).invoke();
                }});
        }
        throw new IllegalArgumentException("Don't know how to coerce "+value.getClass()+" into "+cls);
    }

    private static final Map<Class<?>, Map<String, Method[]>> methods = new HashMap<>();
    private static final Method[] ZERO_METHODS = new Method[]{};

    /** For whatever reason, Class.getMethods() and Method.getName() show up as hot in the profiler, so we cache them to avoid the expensive invocations **/
    private static Method[] getSingleArgumentMethods(Class<?> cls, String name) {
            Map<String, Method[]> methodMap = methods.get(cls);
        if (methodMap == null) {
            methodMap = new HashMap<>();
            for (Method method : cls.getMethods()) {
                String methodName = method.getName();
                if (method.getParameterTypes().length != 1) continue;
                Method[] named = methodMap.get(methodName);
                if (named == null) named = new Method[]{method};
                else {
                    Method[] newNamed = new Method[named.length + 1];
                    System.arraycopy(named, 0, newNamed, 0, named.length);
                    newNamed[named.length] = method;
                    named = newNamed;
                }
                methodMap.put(methodName, named);
            }
            methods.put(cls, methodMap);
        }

        Method[] result = methods.get(cls).get(name);
        return result != null ? result : ZERO_METHODS;
    }

    private static void setAttribute(View view, ViewGroup container, String attribute, Object oldAttributeValue, Object newAttributeValue)
    {
        // Bail out early if the attribute is unchanged
        if(oldAttributeValue == newAttributeValue) return;
        if(oldAttributeValue != null && oldAttributeValue.equals(newAttributeValue)) return;

        // Determine the predicted name for the setter function
        String predictedFunctionName = "set" + Character.toUpperCase(attribute.charAt(0)) + attribute.substring(1);

        // TODO: Can a component define a different attribute with the same unqualified name?  For instance, is `android:padding` different from `app:padding`?
        if(attribute.startsWith("android:")) attribute = attribute.substring("android:".length());

        if(view instanceof TextView && newAttributeValue instanceof String && "textSize".equals(attribute)) {
            // Text view specifies the size in SP, so we coerce PX and then convert to SP
            // We could be smarter about this and just go directly to SP, or better yet, do it at compile time.
            newAttributeValue = coercePixels(view.getContext(), newAttributeValue);
        }

        // Handle case where newAttributeValue is a parameterless lambda that returns a MarkupFragment, and the predicted setter takes in a MarkupFragment but not a parameterless lambda
        // In such a case, we autoexecute the lambda and pass in the MarkupFragment (since this is natural in the Kotlin DSL)
        if(newAttributeValue != null) {
            try {
                Class<?> returnType = newAttributeValue.getClass().getMethod("invoke").getReturnType();
                if (Function0.class.isAssignableFrom(newAttributeValue.getClass()) && (MarkupFragment.class.isAssignableFrom(returnType) || Void.TYPE.equals(returnType))) {
                    boolean methodExists = false;
                    try {
                        view.getClass().getMethod(predictedFunctionName, Function0.class);
                        methodExists = true;
                    } catch (NoSuchMethodException e) { /* expected */}
                    if (!methodExists) {
                        methodExists = false;
                        try {
                            view.getClass().getMethod(predictedFunctionName, MarkupFragment.class);
                            methodExists = true;
                        } catch (NoSuchMethodException e) { /* expected */}
                        if (methodExists)
                            newAttributeValue = ((Function0<MarkupFragment>) newAttributeValue).invoke();
                    }
                }
            } catch (NoSuchMethodException e) {/* do nothing */}
        }

        // Handle case where newAttributeValue is a List<Element>, and the predicted setter takes in a MarkupFragment but not a list
        // In such a case, we autoexecute the lambda and pass in the MarkupFragment (since this is natural in the Kotlin DSL)
        if(newAttributeValue != null) {
            if (List.class.isAssignableFrom(newAttributeValue.getClass())) {
                boolean methodExists = false;
                try {
                    view.getClass().getMethod(predictedFunctionName, List.class);
                    methodExists = true;
                } catch (NoSuchMethodException e) { /* expected */}
                if (!methodExists) {
                    methodExists = false;
                    try {
                        view.getClass().getMethod(predictedFunctionName, MarkupFragment.class);
                        methodExists = true;
                    } catch (NoSuchMethodException e) { /* expected */}
                    if (methodExists)
                        newAttributeValue = new MarkupFragment((List<Element>) newAttributeValue);
                }
            }
        }

        if("paddingLeft".equals(attribute) || "paddingRight".equals(attribute) || "paddingTop".equals(attribute) || "paddingBottom".equals(attribute)) {
            int paddingValue = (newAttributeValue != NO_VALUE_SPECIFIED && newAttributeValue != null) ? (int)coercePixels(view.getContext(), newAttributeValue) : 0;
            if("paddingLeft".equals(attribute)) view.setPadding(paddingValue, view.getPaddingTop(), view.getPaddingRight(), view.getPaddingBottom());
            if("paddingTop".equals(attribute)) view.setPadding(view.getPaddingLeft(), paddingValue, view.getPaddingRight(), view.getPaddingBottom());
            if("paddingRight".equals(attribute)) view.setPadding(view.getPaddingLeft(), view.getPaddingTop(), paddingValue, view.getPaddingBottom());
            if("paddingBottom".equals(attribute)) view.setPadding(view.getPaddingLeft(), view.getPaddingTop(), view.getPaddingRight(), paddingValue);
            return;
        }

        if("elevation".equals(attribute)) {
            newAttributeValue = (newAttributeValue != NO_VALUE_SPECIFIED && newAttributeValue != null) ? coercePixels(view.getContext(), newAttributeValue) : 0f;
        }

        if(view instanceof TextView && "bufferType".equals(attribute)) {
            TextView tv = (TextView)view;
            tv.setText(tv.getText(), (TextView.BufferType) newAttributeValue);
            return;
        }

        if(view instanceof TextView && "textColor".equals(attribute)) {
            TextView tv = (TextView)view;
            if(newAttributeValue instanceof String) {
                tv.setTextColor(Color.parseColor((String)newAttributeValue));
                return;
            }
        }

        if(view instanceof TextView && "text".equals(attribute)) {
            if(newAttributeValue instanceof Character) newAttributeValue = newAttributeValue.toString();
        }

        { // Attempt to call the setter, if an appropriate one exists
            while (predictedFunctionName.contains("_")) {
                int underscoreIndex = predictedFunctionName.indexOf("_");
                predictedFunctionName = predictedFunctionName.substring(0, underscoreIndex) + Character.toUpperCase(predictedFunctionName.charAt(underscoreIndex + 1)) + predictedFunctionName.substring(underscoreIndex + 2);
            }
            Method bestMatch = null;
            for (Method method : getSingleArgumentMethods(view.getClass(), predictedFunctionName)) {
                if(!Modifier.isPublic(method.getModifiers())) continue;

                if (isCoercable(newAttributeValue, method.getParameterTypes()[0])) {
                    bestMatch = method;
                }
            }

            if(bestMatch != null)
                try {
                    bestMatch.invoke(view, newAttributeValue != NO_VALUE_SPECIFIED ? newAttributeValue : null);
                    return;
                } catch(Exception e) {
                    throw new RuntimeException(e);
                }
        }

        // Attributes for all views
        if("onClick".equals(attribute) || "onClickListener".equals(attribute)) {
            view.setOnClickListener(coerceValue(newAttributeValue, View.OnClickListener.class));
            return;
        }
        if("onTouch".equals(attribute) || "onTouchListener".equals(attribute)) {
            view.setOnTouchListener(coerceValue(newAttributeValue, View.OnTouchListener.class));
            return;
        }
        if("layoutParams".equals(attribute)) {
            ((View) view).setLayoutParams((ViewGroup.LayoutParams) newAttributeValue);
            return;
        }

        if(view instanceof Button) {
            try {
                if("enabled".equals(attribute)) {
                    ((Button) view).setEnabled(coerceValue(newAttributeValue, Boolean.class));
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if(view instanceof RadioButton) {
            try {
                if("checked".equals(attribute)) {
                    ((RadioButton) view).setChecked(coerceValue(newAttributeValue, Boolean.class));
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if(view instanceof ImageView) {
            try {
                if("src".equals(attribute))
                {
                    if(newAttributeValue instanceof Integer) {
                        ((ImageView) view).setImageResource((Integer)newAttributeValue);
                        return;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if(view instanceof LinearLayout) {
            try {
                if("orientation".equals(attribute) || "android:orientation".equals(attribute))
                {
                    int orientation = -1;
                    System.out.println("attribute value: "+newAttributeValue+" type: "+newAttributeValue.getClass()+" boolean: "+("vertical".equals(newAttributeValue)));
                    if("horizontal".equals(newAttributeValue)) orientation = LinearLayout.HORIZONTAL;
                    if("vertical".equals(newAttributeValue)) orientation = LinearLayout.VERTICAL;
                    if(newAttributeValue instanceof Integer && ((Integer)newAttributeValue).intValue() == LinearLayout.HORIZONTAL) orientation = LinearLayout.HORIZONTAL;
                    if(newAttributeValue instanceof Integer && ((Integer)newAttributeValue).intValue() == LinearLayout.VERTICAL) orientation = LinearLayout.VERTICAL;
                    if(orientation == -1) throw new IllegalArgumentException("LinearLayout must have orientation value of `horizontal` or `vertical`");
                    ((LinearLayout) view).setOrientation(orientation);
                    return;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
/*        if(view instanceof ConstraintLayout) {
            try {
                if("orientation".equals(attribute) || "android:orientation".equals(attribute))
                {
                    int orientation = -1;
                    System.out.println("attribute value: "+newAttributeValue+" type: "+newAttributeValue.getClass()+" boolean: "+("vertical".equals(newAttributeValue)));
                    if("horizontal".equals(newAttributeValue)) orientation = LinearLayout.HORIZONTAL;
                    if("vertical".equals(newAttributeValue)) orientation = LinearLayout.VERTICAL;
                    if(newAttributeValue instanceof Integer && ((Integer)newAttributeValue).intValue() == LinearLayout.HORIZONTAL) orientation = LinearLayout.HORIZONTAL;
                    if(newAttributeValue instanceof Integer && ((Integer)newAttributeValue).intValue() == LinearLayout.VERTICAL) orientation = LinearLayout.VERTICAL;
                    if(orientation == -1) throw new IllegalArgumentException("LinearLayout must have orientation value of `horizontal` or `vertical`");
                    ((LinearLayout) view).setOrientation(orientation);
                    return;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
*/
        if(view instanceof ViewGroup) {
            try {
                if("children".equals(attribute) && !view.getClass().getSimpleName().contains("WrapperView")) {
                    List<Element> oldChildren = oldAttributeValue == NO_VALUE_SPECIFIED ? Collections.EMPTY_LIST : (List<Element>)oldAttributeValue;
                    List<Element> newChildren = newAttributeValue == NO_VALUE_SPECIFIED ? Collections.EMPTY_LIST : (List<Element>)newAttributeValue;
                    renderIntoWrapperView(oldChildren, newChildren, (ViewGroup)view, 0, ((ViewGroup)view).getChildCount());
                    return;
                }
            }catch(Exception e)
            {
                throw new RuntimeException(e);
            }
        }

        throw new RuntimeException("unclear how to handle: "+attribute+"="+newAttributeValue+" on view "+view);
    }
}
