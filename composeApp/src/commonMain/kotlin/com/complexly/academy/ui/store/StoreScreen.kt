package com.complexly.academy.ui.store

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.complexly.academy.ui.theme.*

/**
 * External store links. Using direct URLs to the Shopify stores.
 */
object StoreLinks {
    const val COMPLEXLY_STORE = "https://complexly.store/"
    const val COMPLEXLY_STORE_COLLECTIONS = "https://complexly.store/collections/complexly"
    const val COMPLEXLY_STORE_CRASHCOURSE = "https://complexly.store/collections/crash-course"
    const val COMPLEXLY_STORE_SCISSHOW = "https://complexly.store/collections/scishow"
    const val COMPLEXLY_STORE_EONS = "https://complexly.store/collections/eons"
    const val COMPLEXLY_STORE_BIZARRE_BEASTS = "https://complexly.store/collections/bizarre-beasts"
    const val COMPLEXLY_STORE_POSTERS = "https://complexly.store/collections/posters"

    // Featured product direct links
    const val PRODUCT_ENDLINGS_HOURGLASS_PIN = "https://complexly.store/products/endlings-hourglass-pin"
    const val PRODUCT_ROCKS_ROCK_SHIRT = "https://complexly.store/products/rocks-rock-shirt"
    const val PRODUCT_DWAYNE_PIN = "https://complexly.store/products/dwayne-pin"
    const val PRODUCT_ROCKS_BOX_SHIRT = "https://complexly.store/products/rocks-box-shirt"
    const val PRODUCT_SCISSHOW_STICKER = "https://complexly.store/products/scishow-sticker"
    const val PRODUCT_SCISSHOW_DICE_SET = "https://complexly.store/products/scishow-dice-set"
    const val PRODUCT_SCIENTIFIC_THINKING_SHIRT = "https://complexly.store/products/scientific-thinking-shirt"
    const val PRODUCT_BIZARRE_BEASTS_PIN_SUB = "https://complexly.store/products/bizarre-beasts-pin-subscription"
    const val PRODUCT_HANDMADE_LOGO_SHEET = "https://complexly.store/products/handmade-logo-sticker-sheet"

    const val GOOD_STORE = "https://good.store/"
    const val GOOD_STORE_COFFEE = "https://good.store/collections/awesome-coffee"
    const val GOOD_STORE_SOCKS = "https://good.store/collections/awesome-socks"
    const val GOOD_STORE_SOAP = "https://good.store/collections/sun-basin-soap"
}

/** A product to display in the store UI. */
data class StoreProduct(
    val title: String,
    val subtitle: String,
    val imageUrl: String,
    val price: String? = null,
    val linkUrl: String,
    val isHero: Boolean = false
)

@Composable
fun StoreScreen(
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Featured products from Complexly Store
    val complexlyFeatured = remember {
        listOf(
            StoreProduct(
                title = "Crash Course Coin",
                subtitle = "Official Crash Course merchandise",
                imageUrl = "https://complexly.store/cdn/shop/files/complexly_icon_748ff6a6-0b09-4797-81a8-bd67ca9f0257.png?crop=center&height=400&v=1757361339&width=400",
                price = null,
                linkUrl = StoreLinks.COMPLEXLY_STORE,
                isHero = true
            ),
        )
    }

    // Featured products — actual products from complexly.store homepage
    val featuredProducts = remember {
        listOf(
            StoreProduct(
                title = "Endlings Hourglass Pin",
                subtitle = "Bizarre Beasts",
                imageUrl = "https://complexly.store/cdn/shop/files/DSF3572.jpg?v=1778103304&width=400",
                price = null,
                linkUrl = StoreLinks.PRODUCT_ENDLINGS_HOURGLASS_PIN
            ),
            StoreProduct(
                title = "Rocks Rock! Shirt",
                subtitle = "CrashCourse",
                imageUrl = "https://complexly.store/cdn/shop/files/DSF1412.jpg?v=1775750496&width=400",
                price = null,
                linkUrl = StoreLinks.PRODUCT_ROCKS_ROCK_SHIRT
            ),
            StoreProduct(
                title = "Dwayne Pin",
                subtitle = "CrashCourse",
                imageUrl = "https://complexly.store/cdn/shop/files/DwaynePin.gif?v=1775752912&width=400",
                price = null,
                linkUrl = StoreLinks.PRODUCT_DWAYNE_PIN
            ),
            StoreProduct(
                title = "Rocks Box Shirt",
                subtitle = "SciShow",
                imageUrl = "https://complexly.store/cdn/shop/files/DSF7928-5.jpg?v=1774561268&width=400",
                price = null,
                linkUrl = StoreLinks.PRODUCT_ROCKS_BOX_SHIRT
            ),
            StoreProduct(
                title = "SciShow Sticker",
                subtitle = "SciShow",
                imageUrl = "https://complexly.store/cdn/shop/files/DSF9677.jpg?v=1774547070&width=400",
                price = null,
                linkUrl = StoreLinks.PRODUCT_SCISSHOW_STICKER
            ),
            StoreProduct(
                title = "SciShow Dice Set",
                subtitle = "SciShow",
                imageUrl = "https://complexly.store/cdn/shop/files/Dice_-_After_LAT.jpg?v=1764001841&width=400",
                price = null,
                linkUrl = StoreLinks.PRODUCT_SCISSHOW_DICE_SET
            ),
            StoreProduct(
                title = "Scientific Thinking Shirt",
                subtitle = "CrashCourse",
                imageUrl = "https://complexly.store/cdn/shop/files/CCST_shirt_badge_photo.jpg?v=1766174878&width=400",
                price = null,
                linkUrl = StoreLinks.PRODUCT_SCIENTIFIC_THINKING_SHIRT
            ),
            StoreProduct(
                title = "Bizarre Beasts Pin Subscription!",
                subtitle = "Bizarre Beasts · Subscription",
                imageUrl = "https://complexly.store/cdn/shop/files/BB_Hawk_-_Common.jpg?v=1777909598&width=400",
                price = null,
                linkUrl = StoreLinks.PRODUCT_BIZARRE_BEASTS_PIN_SUB
            ),
            StoreProduct(
                title = "Handmade Logo Sticker Sheet",
                subtitle = "Complexly",
                imageUrl = "https://complexly.store/cdn/shop/files/Complexly-doodlestickers-site1.jpg?v=1761955288&width=400",
                price = null,
                linkUrl = StoreLinks.PRODUCT_HANDMADE_LOGO_SHEET
            ),
        )
    }

    // Good Store products
    val goodStoreProducts = remember {
        listOf(
            StoreProduct(
                title = "Awesome Coffee",
                subtitle = "Great coffee, 100% profits to charity",
                imageUrl = "https://good.store/cdn/shop/files/EcoGeek_Social_Sharing_Icon_1a509994-4372-47cb-b6c7-447549c9008b.png?v=1741883402",
                price = null,
                linkUrl = StoreLinks.GOOD_STORE_COFFEE
            ),
            StoreProduct(
                title = "Awesome Socks",
                subtitle = "Comfort with purpose",
                imageUrl = "https://good.store/cdn/shop/files/EcoGeek_Social_Sharing_Icon_1a509994-4372-47cb-b6c7-447549c9008b.png?v=1741883402&width=400&height=400",
                price = null,
                linkUrl = StoreLinks.GOOD_STORE_SOCKS
            ),
            StoreProduct(
                title = "Sun Basin Soap",
                subtitle = "Clean skin, clean mission",
                imageUrl = "https://good.store/cdn/shop/files/EcoGeek_Social_Sharing_Icon_1a509994-4372-47cb-b6c7-447549c9008b.png?v=1741883402&width=400&height=400&crop=center",
                price = null,
                linkUrl = StoreLinks.GOOD_STORE_SOAP
            ),
        )
    }

    // Channel collection cards — using channel avatar images from channels.json
    val channelCollections = remember {
        listOf(
            StoreProduct(
                title = "CrashCourse",
                subtitle = "Science courses",
                imageUrl = "https://complexly.org/wp-content/uploads/2024/10/TwitterAvatar.png",
                price = null,
                linkUrl = StoreLinks.COMPLEXLY_STORE_CRASHCOURSE
            ),
            StoreProduct(
                title = "SciShow",
                subtitle = "Explore the unexpected",
                imageUrl = "https://complexly.org/wp-content/uploads/2021/10/SciShow-Prime_Avatar-June2021.jpg",
                price = null,
                linkUrl = StoreLinks.COMPLEXLY_STORE_SCISSHOW
            ),
            StoreProduct(
                title = "PBS Eons",
                subtitle = "History of life on Earth",
                imageUrl = "https://complexly.org/wp-content/uploads/2021/10/EONS_Avatar_2.jpg",
                price = null,
                linkUrl = StoreLinks.COMPLEXLY_STORE_EONS
            ),
            StoreProduct(
                title = "Bizarre Beasts",
                subtitle = "Odd and wonderful creatures",
                imageUrl = "https://complexly.org/wp-content/uploads/2021/10/BEASTS-Avatar.png",
                price = null,
                linkUrl = StoreLinks.COMPLEXLY_STORE_BIZARRE_BEASTS
            ),
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Hero: Complexly Store
        item {
            StoreHeroCard(
                product = complexlyFeatured[0],
                onOpenLink = onOpenLink,
                storeLabel = "complexly.store"
            )
        }

        // Featured Products section
        item {
            SectionHeaderSmall(
                title = "Featured Products",
                subtitle = "From the Complexly Store",
                linkUrl = StoreLinks.COMPLEXLY_STORE,
                onOpenLink = onOpenLink
            )
        }
        // Featured products — 2-column grid
        item {
            FeaturedProductsGrid(
                products = featuredProducts,
                onOpenLink = onOpenLink
            )
        }

        // Divider
        item {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = CardBorder
            )
        }

        // Channel collections carousel
        item {
            SectionHeaderSmall(
                title = "Shop by Channel",
                subtitle = "crashcourse · scishow · eons · bizarre beasts",
                linkUrl = StoreLinks.COMPLEXLY_STORE_COLLECTIONS,
                onOpenLink = onOpenLink
            )
        }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(channelCollections) { product ->
                    StoreProductCard(
                        product = product,
                        channelColor = getStoreColor(product.title),
                        onOpenLink = onOpenLink
                    )
                }
            }
        }

        // Divider
        item {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = CardBorder
            )
        }

        // Good Store section
        item {
            SectionHeaderSmall(
                title = "Good Store",
                subtitle = "socks · coffee · soap — 100% profits to charity",
                linkUrl = StoreLinks.GOOD_STORE,
                onOpenLink = onOpenLink
            )
        }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(goodStoreProducts) { product ->
                    StoreProductCard(
                        product = product,
                        channelColor = CharcoalText,
                        onOpenLink = onOpenLink
                    )
                }
            }
        }
    }
}

/** Hero card at the top of the Store screen. */
@Composable
fun StoreHeroCard(
    product: StoreProduct,
    onOpenLink: (String) -> Unit,
    storeLabel: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clickable { onOpenLink(product.linkUrl) }
    ) {
        // Background image
        AsyncImage(
            model = product.imageUrl,
            contentDescription = product.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )

        // Gradient overlay
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f)
                        ),
                        startY = 80f
                    )
                )
        )

        // Content
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
        ) {
            Text(
                text = storeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = product.title,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = product.subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { onOpenLink(product.linkUrl) },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White,
                    containerColor = Color.White.copy(alpha = 0.15f)
                ),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                modifier = Modifier.height(40.dp)
            ) {
                Icon(Icons.Rounded.ShoppingBag, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Shop Now")
            }
        }
    }
}

/** Horizontal carousel product card. */
@Composable
fun StoreProductCard(
    product: StoreProduct,
    channelColor: Color,
    onOpenLink: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .width(160.dp)
            .clickable { onOpenLink(product.linkUrl) }
    ) {
        // Image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(channelColor.copy(alpha = 0.06f))
        ) {
            AsyncImage(
                model = product.imageUrl,
                contentDescription = product.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            // Subtle overlay for text contrast
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.15f))
            )
        }

        // Title
        Text(
            text = product.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = CharcoalText,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp)
        )

        // Subtitle
        Text(
            text = product.subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MediumGrayText,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
        )
    }
}

/** Compact section header with optional link. */
@Composable
fun SectionHeaderSmall(
    title: String,
    subtitle: String,
    linkUrl: String? = null,
    onOpenLink: ((String) -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = CharcoalText,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MediumGrayText,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        if (linkUrl != null && onOpenLink != null) {
            TextButton(onClick = { onOpenLink(linkUrl) }) {
                Text("Shop All", style = MaterialTheme.typography.labelMedium)
                Icon(Icons.Rounded.ChevronRight, contentDescription = null, Modifier.size(16.dp))
            }
        }
    }
}

/** 2-column grid of featured products with images, titles, vendor, and price. */
@Composable
fun FeaturedProductsGrid(
    products: List<StoreProduct>,
    onOpenLink: (String) -> Unit
) {
    val rows = products.chunked(2)
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        rows.forEach { rowProducts ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowProducts.forEach { product ->
                    FeaturedProductItem(
                        product = product,
                        modifier = Modifier.weight(1f),
                        onOpenLink = onOpenLink
                    )
                }
                // If odd number, add spacer
                if (rowProducts.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun FeaturedProductItem(
    product: StoreProduct,
    modifier: Modifier = Modifier,
    onOpenLink: (String) -> Unit
) {
    Column(
        modifier = modifier
            .clickable { onOpenLink(product.linkUrl) }
    ) {
        // Product image
        AsyncImage(
            model = product.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(12.dp))
        )

        // Title
        Text(
            text = product.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = CharcoalText,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp, start = 2.dp)
        )

        // Vendor subtitle
        Text(
            text = product.subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MediumGrayText,
            maxLines = 1,
            modifier = Modifier.padding(start = 2.dp, top = 2.dp)
        )
    }
}

/** Channel color mapping for store sections. */
fun getStoreColor(name: String): Color {
    return when {
        name.contains("Coin", ignoreCase = true) ||
        name.contains("CrashCourse", ignoreCase = true) ||
        name.contains("Rocks", ignoreCase = true) ||
        name.contains("Dwayne", ignoreCase = true) ||
        name.contains("Scientific Thinking", ignoreCase = true) -> Color(0xFFE67E22)
        name.contains("SciShow", ignoreCase = true) -> Color(0xFF3498DB)
        name.contains("Eons", ignoreCase = true) -> Color(0xFF1ABC9C)
        name.contains("Endlings", ignoreCase = true) ||
        name.contains("Bizarre", ignoreCase = true) -> Color(0xFF9B59B6)
        name.contains("Coffee", ignoreCase = true) -> Color(0xFF8B4513)
        name.contains("Socks", ignoreCase = true) -> Color(0xFF2E86C1)
        name.contains("Soap", ignoreCase = true) -> Color(0xFF27AE60)
        name.contains("Logo", ignoreCase = true) ||
        name.contains("Complexly", ignoreCase = true) -> Color(0xFF5D6D7E)
        else -> MediumGrayText
    }
}
