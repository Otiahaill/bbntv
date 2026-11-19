package com.example.prtv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChannelRefTest {
    private val id = "UCabcdefghijklmnopqrstuv"   // UC + 22 characters

    @Test fun handleWithAt() = assertEquals(ChannelRef.Handle("@PowerRangers"), ChannelRef.parse("@PowerRangers"))
    @Test fun bareNameBecomesHandle() = assertEquals(ChannelRef.Handle("@PowerRangers"), ChannelRef.parse("PowerRangers"))
    @Test fun handleInUrl() =
        assertEquals(ChannelRef.Handle("@PowerRangers"), ChannelRef.parse("https://www.youtube.com/@PowerRangers/videos"))
    @Test fun channelIdRaw() = assertEquals(ChannelRef.Id(id), ChannelRef.parse(id))
    @Test fun channelIdInUrl() = assertEquals(ChannelRef.Id(id), ChannelRef.parse("https://www.youtube.com/channel/$id"))
    @Test fun blankIsNull() = assertNull(ChannelRef.parse("   "))
    @Test fun lonelyAtIsNull() = assertNull(ChannelRef.parse("@"))
}
