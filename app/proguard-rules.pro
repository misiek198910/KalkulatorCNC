# ==============================================================================
# 1. ATRYBUTY I REFLEKSJA (Kluczowe dla Retrofit, Gson i błędu ParameterizedType)
# ==============================================================================
-keepattributes Signature, EnclosingMethod, InnerClasses, *Annotation*, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-dontwarn java.lang.reflect.ParameterizedType

# ==============================================================================
# 2. TWOJE MODELE I KOMUNIKACJA (MivS Ecosystem)
# ==============================================================================

# Chroni modele danych (NewsResponse) i interfejsy API (ApiService)
-keep class com.example.calkulatorcnc.remote.** { *; }
-keep interface com.example.calkulatorcnc.remote.** { *; }

# Chroni encje bazy danych Room i modele narzędzi
-keep class com.example.calkulatorcnc.entity.** { *; }

# Zabezpiecza Activity i UI przed zbyt agresywnym wycinaniem kodu
-keep class com.example.calkulatorcnc.ui.activities.** { *; }
-keep class com.example.calkulatorcnc.ui.adapters.** { *; }

# ==============================================================================
# 3. RETROFIT I GSON (Obsługa JSON z mivs.dev)
# ==============================================================================

# Zabezpiecza adnotacje rzędowe Retrofit
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Reguły dla Gsona (niezbędne do poprawnego rzutowania typów generycznych)
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken
-keep public class * extends com.google.gson.TypeAdapter

# Chroni pola oznaczone @SerializedName (np. image_url, publish_date)
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ==============================================================================
# 4. GOOGLE PLAY BILLING (Subskrypcje i Płatności)
# ==============================================================================
-keep class com.android.billingclient.** { *; }
-keep interface com.android.billingclient.** { *; }

# Zabezpieczenie callbacków bilingowych przed wycięciem
-keepclassmembers class * implements com.android.billingclient.api.PurchasesUpdatedListener {
    public void onPurchasesUpdated(com.android.billingclient.api.BillingResult, java.util.List);
}

# ==============================================================================
# 5. GOOGLE SERVICES (AdMob, Firebase, Analytics)
# ==============================================================================
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.firebase.** { *; }
-keepclassmembers class * {
  @com.google.firebase.firestore.PropertyName <fields>;
}
-keep class com.google.android.gms.measurement.** { *; }
-keep public class com.google.android.gms.common.internal.safeparcel.SafeParcelable {
    public static final *** NULL;
}

# ==============================================================================
# 6. SYSTEMOWE I UI (Room, View, Zasoby R)
# ==============================================================================

# Wsparcie dla Room Database
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.Entity

# Zachowaj wszystkie ID zasobów (naprawia błędy findViewById i błędy lateinit)
-keepclassmembers class **.R$* {
    public static <fields>;
}

# Chroni widoki używane w layoutach XML
-keepclassmembers class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

# Całkowity zakaz dotykania czegokolwiek w pakiecie remote
-keep,allowobfuscation,allowoptimization class com.example.calkulatorcnc.remote.** { *; }
-keep interface com.example.calkulatorcnc.remote.** { *; }

# Zakaz dotykania bibliotek, które używają refleksji
-keep class retrofit2.** { *; }
-keep class com.google.gson.** { *; }
-keepattributes Signature, *Annotation*