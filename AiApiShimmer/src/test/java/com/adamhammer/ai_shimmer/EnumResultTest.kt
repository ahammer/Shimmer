package com.adamhammer.ai_shimmer

import com.adamhammer.ai_shimmer.adapters.OpenAiAdapter
import com.adamhammer.ai_shimmer.adapters.StubAdapter
import org.junit.jupiter.api.Test
import java.util.concurrent.Future
import kotlinx.serialization.Serializable

// Swagger/OpenAPI annotations
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.media.Content
import org.junit.jupiter.api.Assertions.*

// Define an enum for card rank including an UNDEFINED value.
@Serializable
@Schema(title = "CardRank", description = "The rank of a playing card")
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

// Define an enum for card suit including an UNDEFINED value.
@Serializable
@Schema(title = "CardSuit", description = "The suit of a playing card")
enum class CardSuit {
    UNDEFINED,
    HEARTS,
    DIAMONDS,
    CLUBS,
    SPADES
}

// Define the response class which holds the drawn card information.
@Serializable
@Schema(
    title = "HigherCardResponse",
    description = "The card drawn which is higher than the provided card. " +
            "If no higher card is available, both rank and suit will be set to UNDEFINED."
)
data class HigherCardResponse(
    @field:Schema(title = "Rank", description = "The rank of the drawn card")
    val rank: CardRank = CardRank.UNDEFINED,

    @field:Schema(title = "Suit", description = "The suit of the drawn card")
    val suit: CardSuit = CardSuit.UNDEFINED
)

// Define the API interface for drawing a higher card.
interface HigherCardAPI {
    @Operation(
        summary = "Draw Higher Card",
        description = "Draws a card with a higher rank than the given card. " +
                "If no higher card is available, returns a card with undefined rank and suit."
    )
    @ApiResponse(
        description = "The drawn card as an object with rank and suit enums",
        content = [Content(schema = Schema(implementation = HigherCardResponse::class))]
    )
    fun drawHigherCard(
        @Parameter(description = "The current card value as an integer")
        value: Int,

        @Parameter(description = "The current card suit as a card suit enum")
        suit: CardSuit
    ): Future<HigherCardResponse>
}

// Test cases for the HigherCardAPI.
class HigherCardApiTest {

    @Test
    fun testDrawHigherCardDefined() {
        val api = ShimmerBuilder(HigherCardAPI::class)
            .setAdapter(OpenAiAdapter())
            .build()

        // Simulate a scenario where a higher card is available.
        val response = api.drawHigherCard(5, CardSuit.HEARTS).get()
        assertNotNull(response, "Response should not be null")
        // Verify that the response contains a defined card (i.e. not UNDEFINED).
        assertNotEquals(CardRank.UNDEFINED, response.rank, "Expected a defined rank")
        assertNotEquals(CardSuit.UNDEFINED, response.suit, "Expected a defined suit")
    }

    @Test
    fun testDrawHigherCardUndefined() {
        val api = ShimmerBuilder(HigherCardAPI::class)
            .setAdapter(StubAdapter())
            .build()

        // Simulate a scenario where no higher card is available (e.g., highest card provided).
        val response = api.drawHigherCard(13, CardSuit.CLUBS).get() // Assuming 13 represents ACE.
        assertNotNull(response, "Response should not be null")
        // Verify that both enums are set to UNDEFINED when no higher card exists.
        assertEquals(CardRank.UNDEFINED, response.rank, "Expected undefined rank when no higher card is available")
        assertEquals(CardSuit.UNDEFINED, response.suit, "Expected undefined suit when no higher card is available")
    }
}
