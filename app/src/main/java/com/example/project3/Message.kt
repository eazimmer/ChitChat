package com.example.project3

// Object representing a sent message
data class Message(val _id: String,
                   val client: String,
                   val date: String,
                   val dislikes: Int,
                   val likes: Int,
                   val message: String
)