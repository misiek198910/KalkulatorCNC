# 1. Zabezpieczenie Twoich modeli danych i encji Room
# Chroni klasy ThreadEntity, Tool, SubscriptionEntity i ThreadDimension przed zmianą nazw pól
-keep class com.example.calkulatorcnc.entity.** { *; }

# 2. Zabezpieczenie modelu danych dla ActivityNews
# Jeśli Twoja klasa newsów jest w innym pakiecie, upewnij się, że ta ścieżka jest poprawna.
# Zapobiega to błędowi: "No properties to serialize found on class a3.a"
-keep class com.example.calkulatorcnc.ui.activities.** { *; }
-keepattributes *Annotation*
-keepattributes Signature

# 3. Precyzyjne reguły dla Google Play Billing Library
# Naprawia błąd "wiecznie aktywnej" subskrypcji i usuwa ostrzeżenie o 100+ klasach
-keep class com.android.billingclient.api.BillingClient { *; }
-keep class com.android.billingclient.api.BillingClientImpl { *; }
-keep class com.android.billingclient.api.BillingResult { *; }
-keep class com.android.billingclient.api.Purchase { *; }
-keep class com.android.billingclient.api.PurchasesUpdatedListener { *; }
-keep class com.android.billingclient.api.PurchaseHistoryRecord { *; }
-keep class com.android.billingclient.api.SkuDetails { *; }
-keep class com.android.billingclient.api.QueryPurchasesParams { *; }

# Zabezpieczenie callbacków bilingowych
-keepclassmembers class * implements com.android.billingclient.api.PurchasesUpdatedListener {
    public void onPurchasesUpdated(com.android.billingclient.api.BillingResult, java.util.List);
}

# 4. Biblioteki Google (AdMob i Firebase)
# Zapobiega problemom z ładowaniem reklam i danymi z Firestore/Analytics
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.measurement.** { *; }
-keep class com.google.android.gms.internal.measurement.** { *; }
-keep class com.google.android.gms.measurement.AppMeasurementService { *; }
-keep public class com.google.android.gms.common.internal.safeparcel.SafeParcelable {
    public static final *** NULL;
}

# 5. Wsparcie dla Room Database
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.Entity

# 6. Serializacja (jeśli używasz Gson lub Firebase Firestore)
-keepclassmembers class com.example.calkulatorcnc.** {
  @com.google.firebase.firestore.PropertyName <fields>;
  @com.google.gson.annotations.SerializedName <fields>;
}