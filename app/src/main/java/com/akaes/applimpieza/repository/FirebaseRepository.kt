package com.akaes.applimpieza.repository

// repository/FirebaseRepository.kt
import android.content.Context
import android.net.Uri
import com.akaes.applimpieza.models.Client
import com.akaes.applimpieza.models.Contract
import com.akaes.applimpieza.models.Provider
import com.akaes.applimpieza.models.Review
import com.akaes.applimpieza.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
//AGREGAR IMPORT PARA CLOUDINARY
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class FirebaseRepository {

    private val auth = FirebaseAuth.getInstance()
     val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance() // Aunque usaremos Cloudinary, lo mantenemos por si hay otros usos.

    companion object {
        private const val CLOUD_NAME = "dstmmxf3d" // Cambiar por tu cloud name
        private const val API_KEY = "232186682323968" // Cambiar por tu API key
        private const val API_SECRET = "dF6Z0pUre3Ng_VD4hhLOl2VRdR8" // Cambiar por tu API secret

        private var isCloudinaryInitialized = false
    }

    // ===== AUTENTICACIÓN =====

    suspend fun registerUser(email: String, password: String): Result<String> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            Result.success(result.user?.uid ?: "")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginUser(email: String, password: String): Result<String> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Result.success(result.user?.uid ?: "")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    fun signOut() = auth.signOut()

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== ALMACENAMIENTO =====

    suspend fun uploadProfilePhoto(userId: String, imageUri: Uri): Result<String> {
        return suspendCancellableCoroutine { continuation ->
            try {
                // ✅ VERIFICAR QUE CLOUDINARY ESTÉ INICIALIZADO
                if (!isCloudinaryInitialized) {
                    println("DEBUG: ❌ Cloudinary no inicializado")
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(Exception("Cloudinary no está inicializado. Llama a initializeCloudinary() primero.")))
                    }
                    return@suspendCancellableCoroutine
                }
                val publicId = "profile_photos/user_${userId}_${System.currentTimeMillis()}"

                println("DEBUG: Subiendo foto de perfil a Cloudinary...")

                MediaManager.get()
                    .upload(imageUri)
                    .option("public_id", publicId)
                    .option("folder", "applimpieza/profiles")
                    .option("resource_type", "image")
                    .option("quality", "auto")
                    .option("fetch_format", "auto")
                    .callback(object : UploadCallback {
                        override fun onStart(requestId: String) {
                            println("DEBUG: Iniciando subida...")
                        }

                        override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                            val progress = (bytes * 100 / totalBytes).toInt()
                            println("DEBUG: Progreso: $progress%")
                        }

                        override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                            val imageUrl = resultData["secure_url"] as? String
                                ?: resultData["url"] as? String

                            if (imageUrl != null) {
                                println("DEBUG: ✅ Imagen subida exitosamente: $imageUrl")
                                if (continuation.isActive) {
                                    continuation.resume(Result.success(imageUrl))
                                }
                            } else {
                                println("DEBUG: ❌ No se pudo obtener URL")
                                if (continuation.isActive) {
                                    continuation.resume(Result.failure(Exception("No se pudo obtener URL de la imagen")))
                                }
                            }
                        }

                        override fun onError(requestId: String, error: ErrorInfo) {
                            println("DEBUG: ❌ Error Cloudinary: ${error.description}")
                            if (continuation.isActive) {
                                continuation.resume(Result.failure(Exception("Error de Cloudinary: ${error.description}")))
                            }
                        }

                        override fun onReschedule(requestId: String, error: ErrorInfo) {
                            println("DEBUG: 🔄 Reintentando...")
                        }
                    })
                    .dispatch()

            } catch (e: Exception) {
                println("DEBUG: ❌ Error iniciando subida: ${e.message}")
                if (continuation.isActive) {
                    continuation.resume(Result.failure(e))
                }
            }
        }
    }

    suspend fun uploadImageWithCustomName(userId: String, imageUri: Uri, fileName: String): Result<String> {
        return suspendCancellableCoroutine { continuation ->
            try {
                val publicId = "user_images/${userId}/${fileName}_${System.currentTimeMillis()}"

                MediaManager.get()
                    .upload(imageUri)
                    .option("public_id", publicId)
                    .option("folder", "applimpieza/custom")
                    .option("resource_type", "image")
                    .option("quality", "auto")
                    .option("fetch_format", "auto")
                    .callback(object : UploadCallback {
                        override fun onStart(requestId: String) {}
                        override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}

                        override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                            val imageUrl = resultData["secure_url"] as? String
                                ?: resultData["url"] as? String

                            if (imageUrl != null && continuation.isActive) {
                                continuation.resume(Result.success(imageUrl))
                            } else if (continuation.isActive) {
                                continuation.resume(Result.failure(Exception("No se pudo obtener URL")))
                            }
                        }

                        override fun onError(requestId: String, error: ErrorInfo) {
                            if (continuation.isActive) {
                                continuation.resume(Result.failure(Exception("Error: ${error.description}")))
                            }
                        }

                        override fun onReschedule(requestId: String, error: ErrorInfo) {}
                    })
                    .dispatch()

            } catch (e: Exception) {
                if (continuation.isActive) {
                    continuation.resume(Result.failure(e))
                }
            }
        }
    }

    // NUEVO MÉTODO para subir múltiples fotos de galería a Cloudinary
    suspend fun uploadGalleryPhotos(providerId: String, imageUris: List<Uri>): Result<List<String>> {
        return suspendCancellableCoroutine { continuation ->
            if (!isCloudinaryInitialized) {
                if (continuation.isActive) {
                    continuation.resume(Result.failure(Exception("Cloudinary no está inicializado. Llama a initializeCloudinary() primero.")))
                }
                return@suspendCancellableCoroutine
            }

            val uploadedUrls = mutableListOf<String>()
            val totalImages = imageUris.size
            var completedUploads = 0
            val errors = mutableListOf<Throwable>()

            if (totalImages == 0) {
                if (continuation.isActive) {
                    continuation.resume(Result.success(emptyList()))
                }
                return@suspendCancellableCoroutine
            }

            imageUris.forEachIndexed { index, uri ->
                val publicId = "provider_gallery/${providerId}/item_${System.currentTimeMillis()}_$index"

                MediaManager.get()
                    .upload(uri)
                    .option("public_id", publicId)
                    .option("folder", "applimpieza/gallery") // Carpeta específica para la galería
                    .option("resource_type", "image")
                    .option("quality", "auto")
                    .option("fetch_format", "auto")
                    .callback(object : UploadCallback {
                        override fun onStart(requestId: String) {}
                        override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}

                        override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                            val imageUrl = resultData["secure_url"] as? String
                                ?: resultData["url"] as? String

                            if (imageUrl != null) {
                                uploadedUrls.add(imageUrl)
                            } else {
                                errors.add(Exception("No se pudo obtener URL para la imagen en el índice $index"))
                            }
                            completedUploads++
                            if (completedUploads == totalImages) {
                                if (errors.isEmpty() && continuation.isActive) {
                                    continuation.resume(Result.success(uploadedUrls))
                                } else if (continuation.isActive) {
                                    continuation.resume(Result.failure(Exception("Errores al subir algunas imágenes: ${errors.map { it.message }.joinToString()}")))
                                }
                            }
                        }

                        override fun onError(requestId: String, error: ErrorInfo) {
                            errors.add(Exception("Error al subir imagen en el índice $index: ${error.description}"))
                            completedUploads++
                            if (completedUploads == totalImages) {
                                if (continuation.isActive) {
                                    continuation.resume(Result.failure(Exception("Errores al subir algunas imágenes: ${errors.map { it.message }.joinToString()}")))
                                }
                            }
                        }

                        override fun onReschedule(requestId: String, error: ErrorInfo) {}
                    })
                    .dispatch()
            }
        }
    }

    // ===== USUARIOS =====

    suspend fun createUser(user: User): Result<Unit> {
        return try {
            db.collection("users").document(user.id).set(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUser(userId: String): Result<User?> {
        return try {
            val document = db.collection("users").document(userId).get().await()
            val user = document.toObject(User::class.java)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserRole(userId: String, role: String): Result<Unit> {
        return try {
            db.collection("users").document(userId)
                .update("role", role).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserPhoto(userId: String, photoUrl: String): Result<Unit> {
        return try {
            db.collection("users").document(userId)
                .update("photoUrl", photoUrl).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProviderInfo(
        userId: String,
        phone: String,
        description: String,
        serviceTypes: List<String>
    ): Result<Unit> {
        return try {
            // Buscar el documento del proveedor por userId
            val query = db.collection("providers")
                .whereEqualTo("userId", userId)
                .limit(1)

            val documents = query.get().await()

            if (documents.documents.isNotEmpty()) {
                // Actualizar proveedor existente
                val providerId = documents.documents.first().id
                val updates = hashMapOf<String, Any>(
                    "phone" to phone,
                    "description" to description,
                    "serviceTypes" to serviceTypes,
                    "updatedAt" to com.google.firebase.Timestamp.now()
                )

                db.collection("providers").document(providerId)
                    .update(updates).await()
            } else {
                // Crear nuevo proveedor si no existe (no debería pasar)
                val provider = Provider(
                    userId = userId,
                    phone = phone,
                    description = description,
                    serviceTypes = serviceTypes
                )
                createProvider(provider)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== PROVEEDORES =====

    suspend fun createProvider(provider: Provider): Result<Unit> {
        return try {
            val docRef = db.collection("providers").document()
            val providerWithId = provider.copy(id = docRef.id)
            docRef.set(providerWithId).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProvidersByService(serviceType: String): Result<List<Provider>> {
        return try {
            val query = db.collection("providers")
                .whereArrayContains("serviceTypes", serviceType)
                .orderBy("rating", Query.Direction.DESCENDING)

            val documents = query.get().await()
            val providers = documents.toObjects(Provider::class.java)
            Result.success(providers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProviderByUserId(userId: String): Result<Provider?> {
        return try {
            val query = db.collection("providers")
                .whereEqualTo("userId", userId)
                .limit(1)

            val documents = query.get().await()
            val provider = documents.documents.firstOrNull()?.toObject(Provider::class.java)
            Result.success(provider)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== CLIENTES =====

    suspend fun createClient(client: Client): Result<Unit> {
        return try {
            val docRef = db.collection("clients").document()
            val clientWithId = client.copy(id = docRef.id)
            docRef.set(clientWithId).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== CONTRATOS =====

    suspend fun createContract(contract: Contract): Result<String> {
        return try {
            val docRef = db.collection("contracts").document()
            val contractWithId = contract.copy(id = docRef.id)
            docRef.set(contractWithId).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getContractsByProvider(providerId: String): Result<List<Contract>> {
        return try {
            val query = db.collection("contracts")
                .whereEqualTo("providerId", providerId)
                .orderBy("createdAt", Query.Direction.DESCENDING)

            val documents = query.get().await()
            val contracts = documents.toObjects(Contract::class.java)
            Result.success(contracts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateContractStatus(contractId: String, status: String): Result<Unit> {
        return try {
            val updates = hashMapOf<String, Any>(
                "status" to status,
                "updatedAt" to com.google.firebase.Timestamp.now()
            )

            if (status == "completado") {
                updates["completedAt"] = com.google.firebase.Timestamp.now()
            }

            db.collection("contracts").document(contractId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== RESEÑAS =====

    suspend fun createReview(review: Review): Result<Unit> {
        return try {
            val docRef = db.collection("reviews").document()
            val reviewWithId = review.copy(id = docRef.id)
            docRef.set(reviewWithId).await()

            // Actualizar estadísticas del proveedor
            updateProviderRating(review.providerId)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getReviewsByProvider(providerId: String): Result<List<Review>> {
        return try {
            val query = db.collection("reviews")
                .whereEqualTo("providerId", providerId)
                .orderBy("createdAt", Query.Direction.DESCENDING)

            val documents = query.get().await()
            val reviews = documents.toObjects(Review::class.java)
            Result.success(reviews)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun updateProviderRating(providerId: String) {
        try {
            val reviews = getReviewsByProvider(providerId).getOrNull() ?: return

            if (reviews.isNotEmpty()) {
                val averageRating = reviews.map { it.rating }.average()
                val totalReviews = reviews.size

                db.collection("providers").document(providerId)
                    .update(
                        mapOf(
                            "rating" to averageRating,
                            "totalReviews" to totalReviews
                        )
                    ).await()
            }
        } catch (e: Exception) {
            // Log error but don't throw
        }
    }


    // --- MÉTODOS PARA PROVIDER GALLERY ACTUALIZADOS ---

    // Método para agregar un nuevo item a la galería con múltiples fotos
    suspend fun addPhotosToProviderGallery(
        providerId: String,
        photoUrls: List<String>, // Ahora es una lista de URLs
        description: String,
        serviceType: String
    ): Result<String> {
        return try {
            val galleryData = hashMapOf(
                "providerId" to providerId,
                "photoUrls" to photoUrls, // Se almacena como un array
                "description" to description,
                "serviceType" to serviceType,
                "createdAt" to com.google.firebase.Timestamp.now()
            )

            val docRef = db.collection("provider_gallery").document()
            docRef.set(galleryData).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProviderGallery(providerId: String): Result<List<ProviderGalleryItem>> {
        return try {
            if (providerId.isBlank()) {
                return Result.failure(Exception("Provider ID no puede estar vacío"))
            }

            println("DEBUG: Cargando galería para providerId: '$providerId'")

            // Consulta SIN orderBy para evitar problemas de índice
            val snapshot = db.collection("provider_gallery")
                .whereEqualTo("providerId", providerId.trim())
                .get()
                .await()

            println("DEBUG: Documentos encontrados: ${snapshot.documents.size}")

            val galleryItems = mutableListOf<ProviderGalleryItem>()

            for (document in snapshot.documents) {
                try {
                    val item = document.toObject(ProviderGalleryItem::class.java)
                    if (item != null) {
                        // Asegurar que el ID esté establecido
                        val itemWithId = item.copy(id = document.id)
                        galleryItems.add(itemWithId)
                    }
                } catch (e: Exception) {
                    println("DEBUG: Error parseando documento ${document.id}: ${e.message}")
                }
            }

            // Ordenar por fecha en memoria (más recientes primero)
            val sortedItems = galleryItems.sortedByDescending { it.createdAt.seconds }

            println("DEBUG: Items procesados y ordenados: ${sortedItems.size}")
            Result.success(sortedItems)

        } catch (e: Exception) {
            println("DEBUG: Error en getProviderGallery: ${e.message}")
            Result.failure(e)
        }
    }
    // MÉTODO ALTERNATIVO con orderBy (solo usar cuando el índice esté creado)
    suspend fun getProviderGalleryWithOrder(providerId: String): Result<List<ProviderGalleryItem>> {
        return try {
            if (providerId.isBlank()) {
                return Result.failure(Exception("Provider ID no puede estar vacío"))
            }

            println("DEBUG: Cargando galería CON ORDER para providerId: '$providerId'")

            // Esta consulta REQUIERE el índice compuesto
            val snapshot = db.collection("provider_gallery")
                .whereEqualTo("providerId", providerId.trim())
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val galleryItems = snapshot.toObjects(ProviderGalleryItem::class.java)

            // Asegurar que los IDs estén establecidos
            val itemsWithIds = galleryItems.mapIndexed { index, item ->
                item.copy(id = snapshot.documents[index].id)
            }

            println("DEBUG: Items cargados con orden de Firebase: ${itemsWithIds.size}")
            Result.success(itemsWithIds)

        } catch (e: Exception) {
            println("DEBUG: Error en getProviderGalleryWithOrder: ${e.message}")
            Result.failure(e)
        }
    }
    // Método para eliminar un item de la galería (con sus fotos de Cloudinary)
    suspend fun deleteProviderGalleryItem(galleryId: String, photoUrls: List<String>): Result<Unit> {
        return try {
            // Eliminar documento de Firestore
            db.collection("provider_gallery").document(galleryId).delete().await()

            // Eliminar imágenes de Cloudinary
            photoUrls.forEach { url ->
                try {
                    // Cloudinary no tiene un método directo para eliminar por URL en el SDK de Android
                    // Normalmente, se elimina usando el public_id. Necesitarías almacenar el public_id
                    // al subir la imagen o parsearlo de la URL si sigue un patrón consistente.
                    // Por simplicidad en este ejemplo, no se implementa la eliminación de Cloudinary
                    // a menos que Cloudinary.MediaManager.get().deleteResources(publicId) esté disponible
                    // y el publicId se guarde o se pueda extraer de la URL.
                    // Si estás usando Cloudinary para la subida, la eliminación debería gestionarse
                    // a través de su API si es necesario, o manualmente en el panel de Cloudinary.
                    println("DEBUG: Se intentaría eliminar la imagen de Cloudinary: $url (requiere public_id)")
                } catch (e: Exception) {
                    println("DEBUG: Error al intentar eliminar imagen de Cloudinary: ${e.message}")
                    // No fallamos toda la operación si falla una eliminación de imagen.
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== MÉTODOS PARA REVIEWS CON FOTOS =====

    suspend fun createReviewWithPhotos(
        contractId: String,
        clientId: String,
        providerId: String,
        rating: Int,
        comment: String,
        photoUrls: List<String>
    ): Result<String> {
        return try {
            val reviewData = hashMapOf(
                "contractId" to contractId,
                "clientId" to clientId,
                "providerId" to providerId,
                "rating" to rating,
                "comment" to comment,
                "photos" to photoUrls,
                "createdAt" to com.google.firebase.Timestamp.now()
            )

            val docRef = db.collection("reviews").document()
            docRef.set(reviewData).await()

            // Actualizar estadísticas del proveedor
            updateProviderRatingAndJobs(providerId)

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadReviewPhotos(reviewId: String, imageUris: List<Uri>): Result<List<String>> {
        return suspendCancellableCoroutine { continuation ->
            if (!isCloudinaryInitialized) {
                if (continuation.isActive) {
                    continuation.resume(Result.failure(Exception("Cloudinary no está inicializado. Llama a initializeCloudinary() primero.")))
                }
                return@suspendCancellableCoroutine
            }

            val uploadedUrls = mutableListOf<String>()
            val totalImages = imageUris.size
            var completedUploads = 0
            val errors = mutableListOf<Throwable>()

            if (totalImages == 0) {
                if (continuation.isActive) {
                    continuation.resume(Result.success(emptyList()))
                }
                return@suspendCancellableCoroutine
            }

            imageUris.forEachIndexed { index, uri ->
                val publicId = "review_photos/${reviewId}/item_${System.currentTimeMillis()}_$index"

                MediaManager.get()
                    .upload(uri)
                    .option("public_id", publicId)
                    .option("folder", "applimpieza/reviews") // Carpeta específica para reseñas
                    .option("resource_type", "image")
                    .option("quality", "auto")
                    .option("fetch_format", "auto")
                    .callback(object : UploadCallback {
                        override fun onStart(requestId: String) {}
                        override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}

                        override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                            val imageUrl = resultData["secure_url"] as? String
                                ?: resultData["url"] as? String

                            if (imageUrl != null) {
                                uploadedUrls.add(imageUrl)
                            } else {
                                errors.add(Exception("No se pudo obtener URL para la imagen en el índice $index"))
                            }
                            completedUploads++
                            if (completedUploads == totalImages) {
                                if (errors.isEmpty() && continuation.isActive) {
                                    continuation.resume(Result.success(uploadedUrls))
                                } else if (continuation.isActive) {
                                    continuation.resume(Result.failure(Exception("Errores al subir algunas imágenes: ${errors.map { it.message }.joinToString()}")))
                                }
                            }
                        }

                        override fun onError(requestId: String, error: ErrorInfo) {
                            errors.add(Exception("Error al subir imagen en el índice $index: ${error.description}"))
                            completedUploads++
                            if (completedUploads == totalImages) {
                                if (continuation.isActive) {
                                    continuation.resume(Result.failure(Exception("Errores al subir algunas imágenes: ${errors.map { it.message }.joinToString()}")))
                                }
                            }
                        }

                        override fun onReschedule(requestId: String, error: ErrorInfo) {}
                    })
                    .dispatch()
            }
        }
    }


    // ===== MÉTODOS ACTUALIZADOS PARA PROVIDERS =====

    private suspend fun updateProviderRatingAndJobs(providerId: String) {
        try {
            // Obtener todas las reviews del proveedor
            val reviewsQuery = db.collection("reviews")
                .whereEqualTo("providerId", providerId)

            val reviewsSnapshot = reviewsQuery.get().await()
            val reviews = reviewsSnapshot.documents

            if (reviews.isNotEmpty()) {
                val ratings = reviews.mapNotNull { it.getLong("rating")?.toInt() }
                val averageRating = ratings.average()
                val totalReviews = reviews.size

                // Contar trabajos completados
                val contractsQuery = db.collection("contracts")
                    .whereEqualTo("providerId", providerId)
                    .whereEqualTo("status", "completado")

                val contractsSnapshot = contractsQuery.get().await()
                val completedJobs = contractsSnapshot.size()

                // Actualizar provider
                val updates = hashMapOf<String, Any>(
                    "rating" to averageRating,
                    "totalReviews" to totalReviews,
                    "completedJobs" to completedJobs
                )

                db.collection("providers").document(providerId)
                    .update(updates).await()
            }
        } catch (e: Exception) {
            // Log error but don't throw
        }
    }

    // --- MODELOS DE DATOS ACTUALIZADOS ---

    data class ProviderGalleryItem(
        val id: String = "",
        val providerId: String = "",
        val photoUrls: List<String> = emptyList(), // CAMBIADO a List<String>
        val description: String = "",
        val serviceType: String = "",
        val createdAt: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now()
    )

    // ===== MÉTODOS AUXILIARES =====

    suspend fun getProviderIdByUserId(userId: String): Result<String?> {
        return try {
            val query = db.collection("providers")
                .whereEqualTo("userId", userId)
                .limit(1)

            val documents = query.get().await()
            val providerId = documents.documents.firstOrNull()?.id
            Result.success(providerId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getClientIdByUserId(userId: String): Result<String?> {
        return try {
            val query = db.collection("clients")
                .whereEqualTo("userId", userId)
                .limit(1)

            val documents = query.get().await()
            val clientId = documents.documents.firstOrNull()?.id
            Result.success(clientId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // NUEVO MÉTODO - Agregar al final de tu clase
    fun initializeCloudinary(context: Context) {
        try {
            if (!isCloudinaryInitialized) {
                println("DEBUG: 🔄 Inicializando Cloudinary...")

                // Verificar que las credenciales no estén vacías
                if (CLOUD_NAME == "tu_cloud_name_aqui" ||
                    API_KEY == "tu_api_key_aqui" ||
                    API_SECRET == "tu_api_secret_aqui"
                ) {
                    println("DEBUG: ❌ ERROR: Debes configurar las credenciales de Cloudinary")
                    return
                }

                val config = HashMap<String, String>()
                config["cloud_name"] = CLOUD_NAME
                config["api_key"] = API_KEY
                config["api_secret"] = API_SECRET

                MediaManager.init(context, config)
                isCloudinaryInitialized = true
                println("DEBUG: ✅ Cloudinary inicializado correctamente")
                println("DEBUG: Cloud Name: $CLOUD_NAME")
            } else {
                println("DEBUG: ✅ Cloudinary ya estaba inicializado")
            }
        } catch (e: Exception) {
            println("DEBUG: ❌ Error inicializando Cloudinary: ${e.message}")
            e.printStackTrace()
        }
    }
    // 1. Verificar si la colección existe y su estructura
    suspend fun debugCollectionStructure(): Result<List<Map<String, Any>>> {
        return try {
            println("DEBUG: === DIAGNÓSTICO DE COLECCIÓN ===")

            val snapshot = db.collection("provider_gallery")
                .limit(5) // Solo los primeros 5 documentos
                .get()
                .await()

            println("DEBUG: Total documentos en colección: ${snapshot.documents.size}")

            val structures = mutableListOf<Map<String, Any>>()

            snapshot.documents.forEachIndexed { index, document ->
                println("DEBUG: --- Documento $index ---")
                println("DEBUG: ID: ${document.id}")

                document.data?.let { data ->
                    println("DEBUG: Campos encontrados:")
                    data.forEach { (key, value) ->
                        println("DEBUG:   '$key' = $value (${value?.javaClass?.simpleName})")
                        // Verificar si hay espacios o caracteres raros
                        if (key.contains(" ") || key != key.trim()) {
                            println("DEBUG: ⚠️  PROBLEMA: Campo '$key' contiene espacios o caracteres extra")
                        }
                    }
                    structures.add(data)
                } ?: println("DEBUG: Documento sin data")
            }

            Result.success(structures)
        } catch (e: Exception) {
            println("DEBUG: Error en diagnóstico: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    // 2. Probar consulta básica sin filtros
    suspend fun testBasicQuery(): Result<Int> {
        return try {
            println("DEBUG: === PRUEBA CONSULTA BÁSICA ===")

            val snapshot = db.collection("provider_gallery")
                .get()
                .await()

            println("DEBUG: Consulta básica exitosa. Total documentos: ${snapshot.documents.size}")
            Result.success(snapshot.documents.size)
        } catch (e: Exception) {
            println("DEBUG: Error en consulta básica: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    // 3. Probar consulta con whereEqualTo pero sin orderBy
    suspend fun testWhereQuery(providerId: String): Result<Int> {
        return try {
            println("DEBUG: === PRUEBA CONSULTA WHERE ===")
            println("DEBUG: Buscando providerId: '$providerId'")

            val snapshot = db.collection("provider_gallery")
                .whereEqualTo("providerId", providerId)
                .get()
                .await()

            println("DEBUG: Consulta WHERE exitosa. Documentos encontrados: ${snapshot.documents.size}")

            // Mostrar los documentos encontrados
            snapshot.documents.forEach { doc ->
                println("DEBUG: Documento encontrado: ${doc.id}")
                doc.data?.let { data ->
                    println("DEBUG:   providerId = '${data["providerId"]}'")
                }
            }

            Result.success(snapshot.documents.size)
        } catch (e: Exception) {
            println("DEBUG: Error en consulta WHERE: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    // 4. Probar consulta completa (con orderBy)
    suspend fun testFullQuery(providerId: String): Result<Int> {
        return try {
            println("DEBUG: === PRUEBA CONSULTA COMPLETA ===")
            println("DEBUG: Buscando providerId: '$providerId' con orderBy")

            val snapshot = db.collection("provider_gallery")
                .whereEqualTo("providerId", providerId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            println("DEBUG: Consulta COMPLETA exitosa. Documentos: ${snapshot.documents.size}")
            Result.success(snapshot.documents.size)
        } catch (e: Exception) {
            println("DEBUG: Error en consulta COMPLETA: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    // 5. Verificar que el providerId existe en la colección providers
    suspend fun verifyProviderExists(providerId: String): Result<Boolean> {
        return try {
            println("DEBUG: === VERIFICAR PROVIDER EXISTE ===")

            val providerDoc = db.collection("providers")
                .document(providerId)
                .get()
                .await()

            val exists = providerDoc.exists()
            println("DEBUG: Provider $providerId existe: $exists")

            if (exists) {
                providerDoc.data?.let { data ->
                    println("DEBUG: Datos del provider: $data")
                }
            }

            Result.success(exists)
        } catch (e: Exception) {
            println("DEBUG: Error verificando provider: ${e.message}")
            Result.failure(e)
        }
    }

    // 6. Crear un documento de prueba con estructura correcta
    suspend fun createTestDocument(providerId: String): Result<String> {
        return try {
            println("DEBUG: === CREAR DOCUMENTO DE PRUEBA ===")

            val testData = hashMapOf(
                "providerId" to providerId,
                "photoUrls" to listOf("https://example.com/test.jpg"),
                "description" to "Documento de prueba",
                "serviceType" to "Limpieza",
                "createdAt" to com.google.firebase.Timestamp.now()
            )

            val docRef = db.collection("provider_gallery").document()
            testData["id"] = docRef.id

            docRef.set(testData).await()
            println("DEBUG: Documento de prueba creado: ${docRef.id}")

            Result.success(docRef.id)
        } catch (e: Exception) {
            println("DEBUG: Error creando documento de prueba: ${e.message}")
            Result.failure(e)
        }
    }
}