// READ ME FIRST!
//
// Code in this file is shared between the Android and Desktop JVM targets.
// Kotlin's hierarchical multiplatform projects currently
// don't support sharing code depending on JVM declarations.
//
// You can follow the progress for HMPP JVM & Android intermediate source sets here:
// https://youtrack.jetbrains.com/issue/KT-42466
//
// Because of the workaround used, some tooling might not behave as expected.
//
// Resolution errors (expect/actual, red code) in your IDE
// do not indicate a problem with your setup.


package androidx.ui.examples.jetissues.data

import androidx.ui.examples.jetissues.query.IssueQuery
import androidx.ui.examples.jetissues.query.IssuesQuery
import androidx.ui.examples.jetissues.query.type.CustomType
import androidx.ui.examples.jetissues.query.type.IssueState
import androidx.ui.examples.jetissues.query.type.OrderDirection
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.CustomTypeAdapter
import com.apollographql.apollo.api.CustomTypeValue
import com.apollographql.apollo.api.Input
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import okhttp3.OkHttpClient
import org.jetbrains.annotations.TestOnly
import java.lang.NullPointerException
import java.time.Instant
import java.util.*

private fun decode(input: String) = input.toCharArray().map { it + 1 }.joinToString("")

val defaultAuth = decode("/`4/81b6db605e8d6``bdc7ecba8d2/a7/370`20")
val defaultRepo = Pair("JetBrains", "compose-jb")

sealed class Result<out R> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
}

enum class IssuesState(val title: String, val githubState: IssueState) {
    OPEN("Open", IssueState.OPEN),
    CLOSED("Closed", IssueState.CLOSED)
}

data class Issues(
    val nodes: List<IssuesQuery.Node>,
    val cursor: String?,
    val state: IssuesState,
    val order: OrderDirection
)

interface IssuesRepository {
    fun getIssues(state: IssuesState, order: OrderDirection, cursor: String? = null, callback: (Result<Issues>) -> Unit)
    fun getIssue(id: Int, callback: (Result<IssueQuery.Issue>) -> Unit)
}

class UnknownRepo : RuntimeException()
class UnknownIssue : RuntimeException()

private const val baseUrl = "https://api.github.com/graphql"

class IssuesRepositoryImpl(
    val owner: String,
    val name: String,
    val token: String
): IssuesRepository {

    private val client: ApolloClient by lazy {
        val okHttpClient = OkHttpClient.Builder()
            .addNetworkInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "bearer $token")
                    .build()

                chain.proceed(request)
            }
            .build()

        ApolloClient.builder()
            .serverUrl(baseUrl)
            .addCustomTypeAdapter(CustomType.DATETIME, object : CustomTypeAdapter<Date> {
                override fun encode(value: Date): CustomTypeValue<*> {
                    throw UnsupportedOperationException()
                }

                override fun decode(value: CustomTypeValue<*>): Date {
                    val v = value.value
                    if (v is String) {
                        return Date.from(Instant.parse(v))
                    }
                    throw IllegalArgumentException(value.toString())
                }
            })
            .okHttpClient(okHttpClient)
            .build()
    }

    override fun getIssues(
        state: IssuesState,
        order: OrderDirection,
        cursor: String?,
        callback: (Result<Issues>) -> Unit) {
        val query = IssuesQuery(owner, name,
            after = Input.optional(cursor),
            state = state.githubState,
            direction = order)
        client.query(query).enqueue(
            object : ApolloCall.Callback<IssuesQuery.Data>() {
                override fun onFailure(e: ApolloException) {
                    callback(Result.Error(e))
                }
                override fun onResponse(response: Response<IssuesQuery.Data>) {
                    val repo = response.data?.repository
                    if (repo == null) {
                        callback(Result.Error(UnknownRepo()))
                    } else {
                        try {
                            callback(Result.Success(Issues(
                                nodes = repo.issues.nodes!!.map { it!! },
                                cursor = repo.issues.pageInfo.endCursor,
                                state = state,
                                order = order
                            )))
                        } catch (e: NullPointerException) {
                            callback(Result.Error(e))
                        }
                    }
                }
            }
        )
    }

    override fun getIssue(id: Int, callback: (Result<IssueQuery.Issue>) -> Unit) {
        val query = IssueQuery(owner, name, id)
        client.query(query).enqueue(object : ApolloCall.Callback<IssueQuery.Data>() {
            override fun onFailure(e: ApolloException) {
                callback(Result.Error(e))
            }

            override fun onResponse(response: Response<IssueQuery.Data>) {
                val issue = response.data?.repository?.issue
                if (issue == null) {
                    callback(Result.Error(UnknownIssue()))
                } else {
                    callback(Result.Success(issue))
                }
            }
        })

    }
}