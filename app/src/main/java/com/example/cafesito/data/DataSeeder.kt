package com.example.cafesito.data

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataSeeder @Inject constructor(
    private val coffeeDao: CoffeeDao,
    private val userDao: UserDao,
    private val socialDao: SocialDao
) {
    suspend fun seedIfNeeded() {
        if (coffeeDao.getCoffeeById("KOFIO-12453") != null) return

        // 1. Seed Cafés
        val sampleCoffees = listOf(
            Coffee(id = "KOFIO-12453", especialidad = "Especialidad", marca = "Nomad Coffee", paisOrigen = "Colombia", variedadTipo = "Arábica", nombre = "Zarza Aji", descripcion = "Un café excepcional con notas cítricas.", fuentePuntuacion = "SCA", puntuacionOficial = 88.5, notasCata = "Cítrico, Chocolate", formato = "Grano", cafeina = "Media", tueste = "Medio", proceso = "Lavado", ratioRecomendado = "1:15", moliendaRecomendada = "Fina", aroma = 9f, sabor = 9f, retrogusto = 8f, acidez = 9f, cuerpo = 8f, uniformidad = 10f, dulzura = 9f, puntuacionTotal = 88.5, codigoBarras = "123456", imageUrl = "https://picsum.photos/seed/coffee1/800/600", productUrl = ""),
            Coffee(id = "KOFIO-13187", especialidad = "Especialidad", marca = "Hola Coffee", paisOrigen = "Etiopía", variedadTipo = "Geisha", nombre = "Yirgacheffe", descripcion = "Notas florales y jazmín.", fuentePuntuacion = "SCA", puntuacionOficial = 91.0, notasCata = "Floral, Té Verde", formato = "Grano", cafeina = "Baja", tueste = "Ligero", proceso = "Natural", ratioRecomendado = "1:16", moliendaRecomendada = "Media", aroma = 10f, sabor = 9f, retrogusto = 9f, acidez = 10f, cuerpo = 7f, uniformidad = 10f, dulzura = 10f, puntuacionTotal = 91.0, codigoBarras = "789012", imageUrl = "https://picsum.photos/seed/coffee2/800/600", productUrl = "")
        )
        sampleCoffees.forEach { coffeeDao.insertCoffee(it) }

        // 2. Seed Usuarios (Baristas sugeridos)
        val baristas = listOf(
            UserEntity(101, null, "barista_master", "Juan Pérez", "https://i.pravatar.cc/150?u=101", "juan@cafesito.com", "Amante del espresso perfecto."),
            UserEntity(102, null, "coffeelover_99", "Marta García", "https://i.pravatar.cc/150?u=102", "marta@cafesito.com", "Catadora certificada SCA.")
        )
        userDao.insertUsers(baristas)

        // 3. Seed Posts iniciales
        val initialPosts = listOf(
            PostEntity("p1", 101, "https://picsum.photos/seed/post1/800/1000", "¡Empezando el día con un V60 de Colombia!", System.currentTimeMillis() - 3600000),
            PostEntity("p2", 102, "https://picsum.photos/seed/post2/800/800", "Increíble tueste natural el de este mes.", System.currentTimeMillis() - 7200000)
        )
        initialPosts.forEach { socialDao.insertPost(it) }
        
        // 4. Seed Reviews iniciales
        val initialReviews = listOf(
            ReviewEntity(coffeeId = "KOFIO-12453", userId = 101, rating = 4.5f, comment = "Balance perfecto entre acidez y dulzor.", timestamp = System.currentTimeMillis())
        )
        initialReviews.forEach { coffeeDao.upsertReview(it) }
    }
}
