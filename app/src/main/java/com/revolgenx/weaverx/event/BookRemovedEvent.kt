package com.revolgenx.weaverx.event

data class BookRemovedEvent(var ids: List<Long>, var withFiles:Boolean)