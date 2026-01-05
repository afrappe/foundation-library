package foundation.rosenblueth.library.data.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Tests para verificar la serialización y deserialización de CaptureData
 */
class CaptureDataSerializationTest {
    
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    @Test
    fun `test CaptureData serialization`() {
        val captureData = CaptureData(
            id = "test_123",
            title = "Test Book Title",
            author = "Test Author",
            isbn = "978-3-16-148410-0",
            publisher = "Test Publisher",
            publishedYear = 2023,
            pages = 350,
            language = "English",
            description = "Test description",
            lcClassification = "QA76.76",
            deweyClassification = "005.1",
            dcuClassification = "DCU123",
            captureTimestamp = 1234567890L
        )
        
        val jsonString = json.encodeToString(captureData)
        
        assertNotNull(jsonString)
        assert(jsonString.contains("\"title\":\"Test Book Title\""))
        assert(jsonString.contains("\"author\":\"Test Author\""))
        assert(jsonString.contains("\"isbn\":\"978-3-16-148410-0\""))
    }
    
    @Test
    fun `test CaptureData deserialization`() {
        val jsonString = """
            {
                "id": "test_123",
                "title": "Test Book Title",
                "author": "Test Author",
                "isbn": "978-3-16-148410-0",
                "publisher": "Test Publisher",
                "publishedYear": 2023,
                "pages": 350,
                "language": "English",
                "description": "Test description",
                "lcClassification": "QA76.76",
                "deweyClassification": "005.1",
                "dcuClassification": "DCU123",
                "captureTimestamp": 1234567890
            }
        """.trimIndent()
        
        val captureData = json.decodeFromString<CaptureData>(jsonString)
        
        assertEquals("test_123", captureData.id)
        assertEquals("Test Book Title", captureData.title)
        assertEquals("Test Author", captureData.author)
        assertEquals("978-3-16-148410-0", captureData.isbn)
        assertEquals("Test Publisher", captureData.publisher)
        assertEquals(2023, captureData.publishedYear)
        assertEquals(350, captureData.pages)
        assertEquals("English", captureData.language)
        assertEquals("Test description", captureData.description)
        assertEquals("QA76.76", captureData.lcClassification)
        assertEquals("005.1", captureData.deweyClassification)
        assertEquals("DCU123", captureData.dcuClassification)
        assertEquals(1234567890L, captureData.captureTimestamp)
    }
    
    @Test
    fun `test CaptureData list serialization`() {
        val captureList = listOf(
            CaptureData(
                id = "1",
                title = "Book 1",
                author = "Author 1",
                isbn = "111-1-11-111111-1"
            ),
            CaptureData(
                id = "2",
                title = "Book 2",
                author = "Author 2",
                isbn = "222-2-22-222222-2"
            )
        )
        
        val jsonString = json.encodeToString(captureList)
        val deserializedList = json.decodeFromString<List<CaptureData>>(jsonString)
        
        assertEquals(2, deserializedList.size)
        assertEquals("Book 1", deserializedList[0].title)
        assertEquals("Book 2", deserializedList[1].title)
    }
    
    @Test
    fun `test fromBookModel conversion`() {
        val bookModel = BookModel(
            title = "Sample Book",
            author = "Sample Author",
            isbn = "123-4-56-789012-3",
            publisher = "Sample Publisher",
            publishedYear = 2024,
            pages = 200,
            language = "Spanish",
            description = "Sample description",
            lcClassification = "LC123",
            deweyClassification = "123.45",
            dcuClassification = "DCU456"
        )
        
        val captureData = CaptureData.fromBookModel(bookModel)
        
        assertEquals(bookModel.title, captureData.title)
        assertEquals(bookModel.author, captureData.author)
        assertEquals(bookModel.isbn, captureData.isbn)
        assertEquals(bookModel.publisher, captureData.publisher)
        assertEquals(bookModel.publishedYear, captureData.publishedYear)
        assertEquals(bookModel.pages, captureData.pages)
        assertEquals(bookModel.language, captureData.language)
        assertEquals(bookModel.description, captureData.description)
        assertEquals(bookModel.lcClassification, captureData.lcClassification)
        assertEquals(bookModel.deweyClassification, captureData.deweyClassification)
        assertEquals(bookModel.dcuClassification, captureData.dcuClassification)
        assertNotNull(captureData.id)
    }
    
    @Test
    fun `test empty fields serialization`() {
        val captureData = CaptureData(
            id = "empty_test",
            title = "Title Only"
        )
        
        val jsonString = json.encodeToString(captureData)
        val deserialized = json.decodeFromString<CaptureData>(jsonString)
        
        assertEquals("empty_test", deserialized.id)
        assertEquals("Title Only", deserialized.title)
        assertEquals("", deserialized.author)
        assertEquals("", deserialized.isbn)
        assertEquals("", deserialized.lcClassification)
        assertEquals("", deserialized.deweyClassification)
        assertEquals("", deserialized.dcuClassification)
    }
}
