package com.payvue.app.domain.asset.category;

public sealed interface AssetClassificationMarker permits Cash, Coupon, GameItem, GiftCard, Mileage, MusicItem, ShoppingItem {
}
