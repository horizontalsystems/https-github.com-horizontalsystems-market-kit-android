package io.horizontalsystems.marketkit.managers

import io.horizontalsystems.marketkit.models.HsTimePeriod
import io.horizontalsystems.marketkit.models.MarketOverview
import io.horizontalsystems.marketkit.models.MarketOverviewResponse
import io.horizontalsystems.marketkit.providers.HsProvider
import io.reactivex.Single

class MarketOverviewManager(
    private val nftManager: NftManager,
    private val hsProvider: HsProvider
) {

    private fun marketOverview(response: MarketOverviewResponse): MarketOverview =
        MarketOverview(
            globalMarketPoints = response.globalMarketPoints,
            coinCategories = response.coinCategories,
            topPlatforms = response.topPlatforms.map { it.topPlatform },
            collections = mapOf(
                HsTimePeriod.Day1 to nftManager.collectionsFromResponses(response.nft.one_day),
                HsTimePeriod.Week1 to nftManager.collectionsFromResponses(response.nft.seven_day),
                HsTimePeriod.Month1 to nftManager.collectionsFromResponses(response.nft.thirty_day)
            )
        )

    fun marketOverviewSingle(currencyCode: String): Single<MarketOverview> =
        hsProvider.marketOverviewSingle(currencyCode).map { marketOverview(it) }

}
