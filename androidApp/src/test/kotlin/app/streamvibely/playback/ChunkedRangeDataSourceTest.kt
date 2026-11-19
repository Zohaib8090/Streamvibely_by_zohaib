package app.streamvibely.playback

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class ChunkedRangeDataSourceTest {

    @Test
    fun parsesContentRangeTotal() {
        assertEquals(3456789L, ChunkedRangeDataSource.parseContentRangeTotal(mapOf("content-range" to listOf("bytes 0-1048575/3456789"))))
        assertEquals(C.LENGTH_UNSET.toLong(), ChunkedRangeDataSource.parseContentRangeTotal(mapOf("Content-Range" to listOf("bytes 0-10/*"))))
        assertEquals(C.LENGTH_UNSET.toLong(), ChunkedRangeDataSource.parseContentRangeTotal(emptyMap()))
    }
}

