package com.tapshare.app

import com.tapshare.app.model.ShareItem
import com.tapshare.app.model.ShareType
import com.tapshare.app.util.ContentUtils
import org.junit.Assert.*
import org.junit.Test

class ShareItemTest {

    @Test
    fun `text share item has correct display name`() {
        val item = ShareItem(
            type = ShareType.TEXT,
            text = "Hello, world!"
        )
        assertEquals("Hello, world!", item.displayName)
    }

    @Test
    fun `long text is truncated in display name`() {
        val longText = "A".repeat(100)
        val item = ShareItem(
            type = ShareType.TEXT,
            text = longText
        )
        assertEquals(50, item.displayName.length)
    }

    @Test
    fun `url share item has correct display name`() {
        val item = ShareItem(
            type = ShareType.URL,
            text = "https://example.com"
        )
        assertEquals("https://example.com", item.displayName)
    }

    @Test
    fun `file share item uses filename for display`() {
        val item = ShareItem(
            type = ShareType.FILE,
            fileName = "document.pdf"
        )
        assertEquals("document.pdf", item.displayName)
    }

    @Test
    fun `image share item uses filename for display`() {
        val item = ShareItem(
            type = ShareType.IMAGE,
            fileName = "photo.jpg"
        )
        assertEquals("photo.jpg", item.displayName)
    }

    @Test
    fun `multiple files shows count`() {
        val item = ShareItem(
            type = ShareType.MULTIPLE_FILES,
            uris = listOf()
        )
        assertEquals("0 files", item.displayName)
    }

    @Test
    fun `determine share type for images`() {
        assertEquals(ShareType.IMAGE, ContentUtils.determineShareType("image/jpeg"))
        assertEquals(ShareType.IMAGE, ContentUtils.determineShareType("image/png"))
        assertEquals(ShareType.IMAGE, ContentUtils.determineShareType("image/gif"))
    }

    @Test
    fun `determine share type for videos`() {
        assertEquals(ShareType.VIDEO, ContentUtils.determineShareType("video/mp4"))
        assertEquals(ShareType.VIDEO, ContentUtils.determineShareType("video/webm"))
    }

    @Test
    fun `determine share type for text`() {
        assertEquals(ShareType.TEXT, ContentUtils.determineShareType("text/plain"))
        assertEquals(ShareType.TEXT, ContentUtils.determineShareType("text/html"))
    }

    @Test
    fun `determine share type for contacts`() {
        assertEquals(ShareType.CONTACT, ContentUtils.determineShareType("text/x-vcard"))
        assertEquals(ShareType.CONTACT, ContentUtils.determineShareType("text/vcard"))
    }

    @Test
    fun `determine share type for generic files`() {
        assertEquals(ShareType.FILE, ContentUtils.determineShareType("application/pdf"))
        assertEquals(ShareType.FILE, ContentUtils.determineShareType("application/zip"))
    }
}
