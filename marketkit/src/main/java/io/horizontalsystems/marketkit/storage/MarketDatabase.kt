package io.horizontalsystems.marketkit.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import io.horizontalsystems.marketkit.models.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.logging.Logger


@Database(
    entities = [
        Coin::class,
        Platform::class,
        CoinCategory::class,
        CoinPrice::class,
        CoinHistoricalPrice::class,
        ChartPointEntity::class,
        GlobalMarketInfo::class,
        Exchange::class,
        SyncerState::class,
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(DatabaseTypeConverters::class)
abstract class MarketDatabase : RoomDatabase() {
    abstract fun coinDao(): CoinDao
    abstract fun coinCategoryDao(): CoinCategoryDao
    abstract fun coinPriceDao(): CoinPriceDao
    abstract fun coinHistoricalPriceDao(): CoinHistoricalPriceDao
    abstract fun chartPointDao(): ChartPointDao
    abstract fun globalMarketInfoDao(): GlobalMarketInfoDao
    abstract fun exchangeDao(): ExchangeDao
    abstract fun syncerStateDao(): SyncerStateDao

    companion object {

        private val logger = Logger.getLogger("MarketDatabase")

        @Volatile
        private var INSTANCE: MarketDatabase? = null

        fun getInstance(context: Context): MarketDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): MarketDatabase {
            val db = Room.databaseBuilder(context, MarketDatabase::class.java, "marketKitDatabase")
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        val loadedCount = loadInitialCoins(db, context)
                        logger.info("Loaded coins count: $loadedCount")
                    }

                    override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                        super.onDestructiveMigration(db)
                        val loadedCount = loadInitialCoins(db, context)
                        logger.info("Loaded coins count: $loadedCount")
                    }
                })
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries()
                .build()

            // force db creation
            db.query("select 1", null)

            return db
        }

        private fun loadInitialCoins(db: SupportSQLiteDatabase, context: Context): Int {
            val inputStream = context.assets.open("initial_coins_list")
            val bufferedReader = BufferedReader(InputStreamReader(inputStream))
            var insertCount = 0

            try {
                while (bufferedReader.ready()) {
                    val insertStmt: String = bufferedReader.readLine()
                    db.execSQL(insertStmt)
                    insertCount++
                }
            } catch (error: Exception) {
                logger.warning("Error in loadInitialCoins(): ${error.message ?: error.javaClass.simpleName}")
            }

            return insertCount
        }
    }
}
