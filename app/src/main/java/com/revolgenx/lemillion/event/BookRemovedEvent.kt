package com.revolgenx.lemillion.event

data class BookRemovedEvent(var ids: List<Long>, var withFiles:Boolean)