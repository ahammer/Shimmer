package com.adamhammer.shimmer

import com.adamhammer.shimmer.adapters.OpenAiAdapter
import org.junit.jupiter.api.Test
import java.util.concurrent.Future
import kotlinx.serialization.Serializable

import com.adamhammer.shimmer.annotations.AiSchema
import com.adamhammer.shimmer.annotations.AiOperation
import com.adamhammer.shimmer.annotations.AiParameter
import com.adamhammer.shimmer.annotations.AiResponse
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Tag

@Serializable
@AiSchema(title = "CardRank", description = "The rank of a playing card")
enum class CardRank {
    UNDEFINED,
    TWO,
    THREE,
    FOUR,
    FIVE,
    SIX,
    SEVEN,
    EIGHT,
    NINE,
    TEN,
    JACK,
    QUEEN,
    KING,
    ACE
}

@Serializable
@AiSchema(title = "CardSuit", description = "The suit of a playing card")
enum class CardSuit {
    UNDEFINED,
    HEARTS,
    DIAMONDS,
    CLUBS,
    SPADES
}

@Serializable
@AiSchema(
    title = "HigherCardResponse",
    description = "The card drawn which is higher than the provided card. " +
            "If no higher card is available, both rank and suit will be set to UNDEFINED."
)
data class HigherCardResponse(
    @field:AiSchema(title = "Rank", description = "The rank of the drawn card")
    val rank: CardRank = CardRank.UNDEFINED,

    @field:AiSchema(title = "Suit", description = "The suit of the drawn card")
    val suit: CardSuit = CardSuit.UNDEFINED
)

interface HigherCardAPI {
    @AiOperation(
        summary = "Draw Higher Card",
        description = "Draws a card with a higher rank than the given card. " +
                "If no higher card is available, returns a card with undefined rank and suit."
    )
    @AiResponse(
        description = "The drawn card as an object with rank and suit enums",
        responseClass = HigherCardResponse::class
    )
    fun drawHigherCard(
        @AiParameter(description = "The current card value as an integer")
        value: Int,

        @AiParameter(description = "The current card suit as a card suit enum")
        suit: CardSuit
    ): Future<HigherCardResponse>
}

class HigherCardApiTest {

    @Test
    @Tag("live")
    fun testDrawHigherCardDefined() {
        val api = ShimmerBuilder(HigherCardAPI::class)
            .setAdapterClass(OpenAiAdapter::class)
            .build().api

        val response = api.drawHigherCard(5, CardSuit.HEARTS).get()
        assertNotNull(response, "Response should not be null")
        assertNotEquals(CardRank.UNDEFINED, response.rank, "Expected a defined rank")
        assertNotEquals(CardSuit.UNDEFINED, response.suit, "Expected a defined suit")
    }

    @Test
    @Tag("live")
    fun testDrawHigherCardUndefined() {
        val api = ShimmerBuilder(HigherCardAPI::class)
            .setAdapterClass(OpenAiAdapter::class)
            .build().api

        val response = api.drawHigherCard(13, CardSuit.CLUBS).get()
        assertNotNull(response, "Response should not be null")
        assertEquals(CardRank.UNDEFINED, response.rank, "Expected undefined rank when no higher card is available")
        assertEquals(CardSuit.UNDEFINED, response.suit, "Expected undefined suit when no higher card is available")
    }
}
