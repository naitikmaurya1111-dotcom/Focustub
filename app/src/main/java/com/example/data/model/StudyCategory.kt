package com.example.data.model

data class StudyCategory(
    val id: String,
    val name: String,
    val iconName: String
) {
    companion object {
        val ALL_CATEGORIES = listOf(
            StudyCategory("all", "All Subjects", "School"),
            StudyCategory("cs", "Computer Science", "Terminal"),
            StudyCategory("math", "Mathematics", "Calculate"),
            StudyCategory("physics", "Physics", "Science"),
            StudyCategory("chemistry", "Chemistry", "Biotech"),
            StudyCategory("engineering", "Engineering", "Build"),
            StudyCategory("biology", "Biology", "Psychology"),
            StudyCategory("philosophy", "Philosophy & Mind", "AutoStories"),
            StudyCategory("economics", "Economics & Finance", "ShowChart")
        )
    }
}
