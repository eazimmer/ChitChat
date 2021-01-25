package com.example.project3

import android.os.AsyncTask
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull


class MainActivity : AppCompatActivity() {

    // Initializing RecyclerView components
    private lateinit var recyclerView: RecyclerView
    private var adapter: RecyclerViewAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Find RecyclerView
        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(applicationContext)

        // Credentials
        val email = "eric.zimmer@mymail.champlain.edu"
        val api_key = "10bde899-67f9-460f-83d2-d0b7fe2ebdd3"
        val base_url = "https://www.stepoutnyc.com/chitchat"

        // Construction of URL for HTTP requests
        // https://www.vogella.com/tutorials/JavaLibrary-OkHttp/article.html
        val urlBuilder = base_url.toHttpUrlOrNull()!!.newBuilder()
        urlBuilder.addQueryParameter("key", api_key)
        urlBuilder.addQueryParameter("client", email)
        val url = urlBuilder.build().toString()

        // Initial data pull - GET Request
        val client = GetRequest(url).execute().get() // https://stackoverflow.com/questions/9273989/how-do-i-retrieve-the-data-from-asynctasks-doinbackground/14129332#14129332
        updateUI(client)

        // Refresh feed
        val refreshButton = findViewById<Button>(R.id.refresh)
        refreshButton.setOnClickListener {
            val refreshedData = GetRequest(url).execute().get()
            updateUI(refreshedData)
        }

        // Send message - POST request
        val postButton = findViewById<Button>(R.id.post)
        postButton.setOnClickListener {
            val message = findViewById<EditText>(R.id.post_field).text.toString() // Get user entered message
            PostRequest(message).execute().get()

            // Refresh to see new post
            val refreshedData = GetRequest(url).execute().get()
            updateUI(refreshedData)
        }
    }

    private fun updateUI(messages: MutableList<Message>) {
        adapter = RecyclerViewAdapter(messages)
        recyclerView.adapter = adapter
    }

    // RecyclerViewHolder
    private inner class MessageHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {

         // Individual Message object
        private lateinit var message: Message

        // Relevant fields of target view
        private val messageView: TextView = itemView.findViewById(R.id.message)
        private val clientView: TextView = itemView.findViewById(R.id.client)
        private val dateView: TextView = itemView.findViewById(R.id.date)
        private val likesView: Button = itemView.findViewById(R.id.likes)
        private val dislikesView: Button = itemView.findViewById(R.id.dislikes)


        // Execute on initialization
        init {
            itemView.setOnClickListener(this) // Unused

            // Like button clicked
            likesView.setOnClickListener {
                // Grab existing data on which messages have been liked
                val savedList = getSharedPreferences("liked", MODE_PRIVATE).getString("liked", null)
                if (savedList != null) {
                    val items = savedList.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toMutableList() // Split array-represented-by-string back down into string

                    // Recognize message as liked, if not yet liked
                    if(!items.contains(message._id)) {
                        items.add(message._id)

                        // Reconstruct string out of array
                        val newList = StringBuilder()
                        for (id in items) {
                            newList.append(id)
                            newList.append(",")
                        }

                        // Update shared preferences with liked messages
                        getSharedPreferences("liked", MODE_PRIVATE).edit()
                            .putString("liked", newList.toString()).apply()

                        // GET Request to Like target message
                        InteractRequest(true, message._id).execute().get()
                        likesView.text = (likesView.text.toString().toInt() + 1).toString() // Manually update personal view
                    }

                } else { // Edge case handling the scenario that SharedPreferences is empty, or cleared
                    val newList = StringBuilder()
                    newList.append(message._id)
                    newList.append(",")

                    getSharedPreferences("liked", MODE_PRIVATE).edit()
                        .putString("liked", newList.toString()).apply()

                    InteractRequest(true, message._id).execute().get()
                    likesView.text = (likesView.text.toString().toInt() + 1).toString()
                }
            }


            // Dislike button clicked - functionally identical to above onClick
            dislikesView.setOnClickListener {
                val savedList = getSharedPreferences("disliked", MODE_PRIVATE).getString("disliked", null)
                if (savedList != null) {
                    val items = savedList.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toMutableList()

                    if(!items.contains(message._id)) {
                        items.add(message._id)

                        val newList = StringBuilder()
                        for (id in items) {
                            newList.append(id)
                            newList.append(",")
                        }

                        getSharedPreferences("disliked", MODE_PRIVATE).edit()
                            .putString("disliked", newList.toString()).apply()

                        InteractRequest(false, message._id).execute().get()
                        dislikesView.text = (dislikesView.text.toString().toInt() + 1).toString()
                    }

                } else {
                    val newList = StringBuilder()
                    newList.append(message._id)
                    newList.append(",")

                    getSharedPreferences("disliked", MODE_PRIVATE).edit()
                        .putString("disliked", newList.toString()).apply()

                    InteractRequest(false, message._id).execute().get()
                    dislikesView.text = (dislikesView.text.toString().toInt() + 1).toString()
                }
            }
        }


        // Bind object data into target view
        fun bind(message: Message) {
            this.message = message
            clientView.text = this.message.client
            messageView.text = this.message.message
            dateView.text = this.message.date
            likesView.text = this.message.likes.toString()
            dislikesView.text = this.message.dislikes.toString()
        }

        // Unused
        override fun onClick(v: View) {
        }
    }


    // RecyclerViewAdapter
    private inner class RecyclerViewAdapter(var messages: MutableList<Message>) : RecyclerView.Adapter<MessageHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
                : MessageHolder {
            val view = layoutInflater.inflate(R.layout.message, parent, false)
            return MessageHolder(view)
        }

        override fun onBindViewHolder(holder: MessageHolder, position: Int) {
            val message = messages[position]
            holder.bind(message)
        }

        override fun getItemCount() = messages.size
    }
}


// For AsyncTask
// https://stackoverflow.com/questions/24399294/android-asynctask-to-make-an-http-get-request
class GetRequest(val url: String) : AsyncTask<String, Int?, MutableList<Message>>() {
    override fun doInBackground(vararg params: String?): MutableList<Message> {
        // Initialize HTTP Client object, and JSON Handler
        val client = OkHttpClient()
        val gson = Gson()

        // Build URL request
        val request = Request.Builder()
            .url(url)
            .build()

        // Receive response
        val response: Response = client.newCall(request).execute()

        // Process response
        val result = response.body?.string() // Initial string response
        val messages = mutableListOf<String>() // Initialize list of Strings
        var messages_objects = mutableListOf<Message>() // Initialize list of Message objects
        var opening: Int = 0 // Helper variable for parsing response; signifies beginning of a message
        var closing: Int = 0 // Helper variable for parsing response; signifies end of a message

        // Splice out individual messages and append into messages variable
        if (result != null) {
            for (i in result.indices) {

                // Beginning of message identified
                if (result[i] == '_') {
                    opening = i-2
                }

                // End of message identified
                else if (result[i] == '}') {
                    closing = i+1
                    messages.add(result.substring(opening, closing))

                    // Once message spliced out, reset counters
                    opening = 0
                    closing = 0
                }
            }
        }

        // Translate string messages into object form
        for (message in messages) {
            messages_objects.add(gson.fromJson(message, Message::class.java))
        }

        return messages_objects
    }
}

// POST Request: Send message
class PostRequest(val message: String) : AsyncTask<String, Int?, Void>() {
    override fun doInBackground(vararg params: String?): Void? {
        // Construct URL
        val client = OkHttpClient()
        val base_url = "https://www.stepoutnyc.com/chitchat"
        val urlBuilder = base_url.toHttpUrlOrNull()!!.newBuilder()
        urlBuilder.addQueryParameter("key", "10bde899-67f9-460f-83d2-d0b7fe2ebdd3")
        urlBuilder.addQueryParameter("client", "eric.zimmer@mymail.champlain.edu")
        val url = urlBuilder.build().toString()

        // Place message payload into form body
        val formBody: RequestBody = FormBody.Builder() // Found here: https://www.baeldung.com/okhttp-post
            .add("message", message)
            .build()

        // Finalize request
        val request = Request.Builder()
            .url(url)
            .post(formBody)
            .build()

        // Execute request
        client.newCall(request).execute()
        return null
    }
}


// GET Request: Like or Dislike a message
class InteractRequest(val liked: Boolean, val message_id: String) : AsyncTask<String, Int?, Void?>() {
    override fun doInBackground(vararg params: String?): Void? {
        // Begin constructing URL
        val client = OkHttpClient()
        var base_url = "https://www.stepoutnyc.com/chitchat"

        // If true, liking. If false, disliking.
        if (liked) {
            base_url += "/like/$message_id"
        } else {
            base_url += "/dislike/$message_id"
        }

        // Finish constructing URL
        val urlBuilder = base_url.toHttpUrlOrNull()!!.newBuilder()
        urlBuilder.addQueryParameter("key", "10bde899-67f9-460f-83d2-d0b7fe2ebdd3")
        urlBuilder.addQueryParameter("client", "eric.zimmer@mymail.champlain.edu")
        val url = urlBuilder.build().toString()

        val request = Request.Builder()
            .url(url)
            .build()

        // Execute request
        client.newCall(request).execute()

        return null
    }
}
