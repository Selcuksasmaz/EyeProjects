Göz Bandı Kalite Kontrol Otomasyon Programı
Okulumuzun düzenlediği Görüntü İşleme ve Mobil Uygulama Hackathonu kapsamında geliştirdiğimiz Göz Bandı Kalite Kontrol Otomasyon Programı,
seri üretimden geçen göz bantlarının ambalajlanmasından sonra herhangi bir kalite sorununun olup olmadığını kontrol etmek için tasarlanmıştır.
Bu uygulama, ürünlerin üzerinde oluşabilecek lekeler, deformasyonlar,kenar simetrisi bozuklukları ve barkod hatalarını tespit ederek kalite kontrol sürecini otomatikleştirir.

Özellikler
Kamera ile gerçek zamanlı görüntü işleme.
Ürün üzerindeki lekeler, deformasyon, kenar simetrisi, yırtık ve barkod tespiti.
Hata eşiği belirleme ve hata oranı hesaplama.

Gereksinimler
Android Studio
Android 8.0 (Oreo) ve sonrasında çalışır.
OpenCV kütüphanesi: OpenCV Android SDK 4.11.0 (https://opencv.org/releases/)
Cihazda kamera erişimi için izinler.

Gerekli Bağımlılıkların Yüklenmesi
Android Studioyu açın ve Githubdan projeyi klonlayın
Daha sonrasında indirdiğiniz OpenCV kütüphanesinin SDK dosyasını projeye yüklemek için File'dan yeni bir modül yüklemeye gelin ve OpenCV kütüphanesini indirmiş olduğunuz SDK Dosyasını Yükleyin
Gradle ile projenizi senkronize edin 
Buradan sonra hata aldıysanız project errors kısmında android studionun belirlediği compileSdk ve targetSdk değerlerini girin.

*** Programı telefonunuza kablo ile aktarıp kullanınız.

Zaman Karmaşıklığı (Time Complexity)
Genel Zaman Karmaşıklığı = O(n * m)

Görüntü Boyutunu Yeniden Ölçekleme = O(n * m) Resmi yeniden boyutlandırmak için her piksel üzerinde işlem yapılır.
Renk Dönüşümü = O(n * m) Renkli görüntüyü gri tonlamaya dönüştürmek için her pikselin renk değeri işlenir.
Bulanıklaştırma =  O(n * m) Görüntüdeki her piksel için komşuluk hesaplamaları yapılır.
Kenar Tespiti = O(n * m) Her piksel için kenar tespiti yapılır.
Kontur Tespiti = O(n * m) Her pikselin kenarları tespit edilir ve her kontur için işlem yapılır. Kontur sayısına bağlı olarak bu adımın karmaşıklığı değişebilir, ancak genellikle O(n * m) civarındadır.
Geometrik Şekil ve Barkod Filtreleme = O(k) burada k, işlenen kontur sayısıdır. Bu adımda, her bir kontur için aspect ratio, solidity gibi geometrik hesaplamalar yapılır. Eğer çok fazla kontur varsa, bu işlem daha karmaşık hale gelebilir.
Defekt Maskesi ve İstatistiksel Analiz = O(n * m) Gri tonlamalı görüntü ile defekt maskesi arasındaki farkları hesaplamak ve standart sapma gibi istatistiksel analizleri yapmak için her piksel işlenir.
Konveksite Defektleri =  O(k) Konveks defektleri tespit etmek için her kontur üzerinde işlem yapılır. Burada k, tespit edilen konturların sayısına bağlıdır.

Mekân Karmaşıklığı
Genel Mekân Karmaşıklığı = O(n * m)

Görüntü Veri Yapıları = Mat rgbaFull, rgba, gray, edges, blurred gibi nesneler her biri O(n * m) bellek kullanır, çünkü her bir Mat nesnesi görüntünün her pikseli için bir değer saklar.
Geçici Veri Yapıları = Mat mask, diff, defectMask, meanDev, stddev, hull, defectsMat gibi geçici veri yapıları da O(n * m) bellek kullanır, çünkü her biri bir görüntüyü (veya bir kısmını) tutar.
Kontur ve Hiyerarşi = List<MatOfPoint> contours ve Mat hierarchy gibi veri yapıları, her kontur için bellek gerektirir. Genellikle kontur sayısı, O(k) (k, tespit edilen konturların sayısı) kadar belleğe ihtiyaç duyar. Bu nedenle, konturlar için kullanılan bellek, genellikle O(k) olarak kabul edilir, ancak genellikle O(n) civarındadır, çünkü her pikselin bir kontur ile ilişkilendirilmesi gereklidir.
Barkod ve Metin Analizi = Barkod ve metin analizi sırasında, geometrik özelliklerin hesaplanması için kullanılan geçici veriler (örneğin, Rect, RotatedRect, MatOfPoint2f) de bellek kullanır, ancak bu genellikle O(k) (kontur sayısı kadar) kadar bir bellek kullanımına sahiptir.
 



