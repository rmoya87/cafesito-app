export type Locale = "es" | "en" | "fr" | "pt" | "de";

export type I18nKey =
  | "common.reload"
  | "common.back"
  | "common.cancel"
  | "common.save"
  | "common.saving"
  | "common.loading"
  | "common.close"
  | "common.next"
  | "common.register"
  | "root.errorTitle"
  | "root.renderError"
  | "nav.main"
  | "nav.home"
  | "nav.search"
  | "nav.brewlab"
  | "nav.diary"
  | "nav.profile"
  | "login.mainAria"
  | "login.joinCommunity"
  | "login.signUpGoogle"
  | "login.termsPrefix"
  | "login.privacy"
  | "login.terms"
  | "login.dataDeletion"
  | "login.welcome"
  | "login.subtitle"
  | "login.benefits"
  | "login.manageTitle"
  | "login.manageDesc"
  | "login.exploreTitle"
  | "login.exploreDesc"
  | "login.brewTitle"
  | "login.brewDesc"
  | "login.trackTitle"
  | "login.trackDesc"
  | "login.startNow"
  | "login.desktopAccess"
  | "login.logoAlt"
  | "cookie.aria"
  | "cookie.title"
  | "cookie.description"
  | "cookie.privacyMore"
  | "cookie.essentialOnly"
  | "cookie.acceptAll"
  | "google.connecting"
  | "notFound.aria"
  | "notFound.kicker"
  | "notFound.title"
  | "notFound.body"
  | "notFound.goHome"
  | "notFound.randomCoffee"
  | "top.search.back"
  | "top.search.users.placeholder"
  | "top.search.users.aria"
  | "top.search.aria"
  | "top.search.placeholder"
  | "top.search.scan"
  | "top.search.filtersAria"
  | "top.filter.country"
  | "top.filter.specialty"
  | "top.filter.roast"
  | "top.filter.format"
  | "top.filter.rating"
  | "top.createCoffee"
  | "top.selectCoffee"
  | "top.saveLabel"
  | "top.goBrewing"
  | "top.goResult"
  | "top.nextConsumption"
  | "top.prevMonth"
  | "top.prev"
  | "top.nextMonth"
  | "top.next"
  | "top.selectPeriod"
  | "top.myDiary"
  | "top.history"
  | "top.followers"
  | "top.following"
  | "top.favorites"
  | "top.listDefault"
  | "top.joinThisList"
  | "top.join"
  | "top.listOptions"
  | "top.searchUsers"
  | "top.profile"
  | "top.profileOptions"
  | "top.general"
  | "top.account"
  | "top.coffeesConsumed"
  | "top.editProfile"
  | "top.language"
  | "top.deleteAccountData"
  | "top.signOut"
  | "top.deleteAccountAria"
  | "top.deleteAccountText"
  | "top.processing"
  | "top.delete"
  | "coffee.notFound"
  | "account.reactivatedBanner"
  | "share.listTitle"
  | "share.listText"
  | "top.coffee"
  | "top.removeFromLists"
  | "top.addToLists"
  | "top.addStock"
  | "top.notifications"
  | "search.followersFollowing"
  | "search.following"
  | "search.follow"
  | "search.noUsersFound"
  | "search.findFriends"
  | "search.recentSearches"
  | "search.clear"
  | "search.coffeeProfile"
  | "search.seeAll"
  | "search.brandFallback"
  | "search.noCoffeeWithFilters"
  | "search.filters"
  | "search.clearFilters"
  | "search.anyRating"
  | "search.minRating"
  | "notifications.today"
  | "notifications.yesterday"
  | "notifications.last7"
  | "notifications.last30"
  | "notifications.title"
  | "notifications.empty"
  | "notifications.inviteGroup"
  | "notifications.declineInvite"
  | "notifications.addList"
  | "notifications.following"
  | "notifications.follow"
  | "notifications.reply"
  | "root.unexpectedError"
  | "auth.access"
  | "tour.skipAll"
  | "tour.gotIt"
  | "lists.join.aria"
  | "lists.join.invited"
  | "lists.join.ownerBy"
  | "lists.join.joining"
  | "lists.join.join"
  | "lists.join.notFound"
  | "lists.join.notFoundHint"
  | "lists.newList"
  | "lists.createList"
  | "lists.creating"
  | "lists.editList"
  | "lists.listName"
  | "lists.listNamePlaceholder"
  | "lists.privacy"
  | "lists.allowEdit"
  | "lists.allowEditAria"
  | "lists.errorCreate"
  | "lists.errorSave"
  | "lists.general"
  | "lists.deleteList"
  | "lists.leaveList"
  | "lists.allowInvite"
  | "lists.allowInviteAria"
  | "profile.noFollowers"
  | "profile.notFollowingAnyone"
  | "scanner.aria"
  | "scanner.title"
  | "scanner.httpsRequired"
  | "scanner.browserNoCamera"
  | "scanner.permissionDenied"
  | "scanner.noCompatibleCamera"
  | "scanner.openFailed"
  | "scanner.hint"
  | "timeline.brewWith"
  | "timeline.prev"
  | "timeline.next"
  | "timeline.invalidValues"
  | "timeline.totalGreaterThanZero"
  | "timeline.remainingNonNegative"
  | "timeline.remainingNoMoreThanTotal"
  | "timeline.yourPantry"
  | "timeline.options"
  | "timeline.addCoffeeToPantry"
  | "timeline.dailyRecommendations"
  | "timeline.peopleToFollow"
  | "timeline.followersOnly"
  | "timeline.pantryOptions"
  | "timeline.organize"
  | "timeline.editStock"
  | "timeline.coffeeFinished"
  | "timeline.deleteFromPantry"
  | "timeline.finishedCoffeeTitle"
  | "timeline.finishedCoffeeText"
  | "timeline.confirm"
  | "timeline.deletingFromPantryTitle"
  | "timeline.deletingFromPantryText"
  | "timeline.deleting"
  | "timeline.editStockTitle"
  | "timeline.totalCoffeeGrams"
  | "timeline.remainingCoffeeGrams"
  | "timeline.cancelUpper"
  | "timeline.saveUpper"
  | "timeline.savingUpper"
  | "brew.method"
  | "brew.methodAria"
  | "brew.selectCoffee"
  | "brew.paramsMethod"
  | "brew.waterMl"
  | "brew.coffeeG"
  | "brew.timeS"
  | "brew.baristaTips"
  | "brew.seeBaristaTips"
  | "brew.timer"
  | "brew.timerAria"
  | "brew.timerEnableAria"
  | "brew.nextLabel"
  | "brew.finish"
  | "brew.total"
  | "brew.restart"
  | "brew.pause"
  | "brew.start"
  | "brew.resultTasteTitle"
  | "brew.recommendation"
  | "brew.configureCoffee"
  | "brew.coffeeType"
  | "brew.cupSize"
  | "brew.result"
  | "brew.registerWater"
  | "brew.apply"
  | "coffee.back"
  | "coffee.closeDetail"
  | "coffee.share"
  | "coffee.brandFallback"
  | "coffee.ratingAria"
  | "coffee.ratingTitle"
  | "coffee.noDescription"
  | "coffee.technicalDetails"
  | "coffee.sensoryProfile"
  | "coffee.edit"
  | "coffee.sensoryBasedOn"
  | "coffee.acquire"
  | "coffee.opinions"
  | "coffee.add"
  | "coffee.yourReview"
  | "coffee.reviewImageAlt"
  | "coffee.editSensoryProfile"
  | "coffee.saveSensoryError"
  | "coffee.saveStockError"
  | "coffee.reviewSheetAria"
  | "coffee.reviewTitle"
  | "coffee.selectRating"
  | "coffee.reviewPlaceholder"
  | "coffee.attachImage"
  | "coffee.reviewPreviewAlt"
  | "coffee.removeImage"
  | "coffee.delete"
  | "coffee.deleting"
  | "coffee.publish"
  | "coffee.publishing"
  | "coffee.selectRatingError"
  | "coffee.writeReviewError"
  | "coffee.addToList"
  | "coffee.createList"
  | "coffee.favorites"
  | "coffee.applyChanges"
  | "coffee.applying"
  | "coffee.addOrRemoveFromList"
  | "diary.selectPeriod"
  | "diary.periodToday"
  | "diary.periodWeek"
  | "diary.periodMonth"
  | "diary.prevMonth"
  | "diary.nextMonth"
  | "diary.select"
  | "diary.goToday"
  | "diary.logWater"
  | "diary.water"
  | "diary.log"
  | "diary.amountMl"
  | "diary.selectCoffee"
  | "diary.yourPantry"
  | "diary.pantryEmpty"
  | "diary.searchCoffeeBrand"
  | "diary.searchCoffeeBrandAria"
  | "diary.scanBarcode"
  | "diary.suggestions"
  | "diary.createMyCoffee"
  | "diary.cancel"
  | "diary.close"
  | "profile.changePhoto"
  | "profile.name"
  | "profile.bio"
  | "profile.following"
  | "profile.follow"
  | "profile.saving"
  | "profile.save"
  | "profile.stats"
  | "profile.seguidores"
  | "profile.tabProfile"
  | "profile.tabActivity"
  | "profile.tabAdn"
  | "profile.tabLists"
  | "profile.activityOfThisUser"
  | "profile.activityMineAndFollowing"
  | "profile.activity.review.other"
  | "profile.activity.review.own"
  | "profile.activity.diary.other"
  | "profile.activity.diary.own"
  | "profile.activity.favorite.other"
  | "profile.activity.favorite.own"
  | "profile.activity.listFallback"
  | "profile.activity.viewListAria"
  | "profile.activity.ratingAndReviewAria"
  | "profile.activity.coffeeImageAlt"
  | "profile.preferenceAnalysis"
  | "diary.habit"
  | "diary.consumption"
  | "diary.barista"
  | "diary.activity"
  | "diary.cups"
  | "diary.cupSize"
  | "diary.method"
  | "diary.coffeeDay"
  | "diary.moment"
  | "diary.morning"
  | "diary.afternoon"
  | "diary.night"
  | "diary.caffeine"
  | "diary.dosePerCoffee"
  | "diary.format"
  | "diary.pantryForecast"
  | "diary.daysApprox"
  | "diary.coffeesTried"
  | "diary.viewCoffeesTried"
  | "diary.roastersTried"
  | "diary.favoriteOrigin"
  | "diary.firstTime"
  | "diary.pantryOptions"
  | "diary.organize"
  | "diary.general"
  | "diary.editStock"
  | "diary.coffeeFinished"
  | "diary.removeFromPantry"
  | "diary.confirm"
  | "diary.removing"
  | "diary.delete"
  | "diary.editEntry"
  | "diary.edit"
  | "diary.timeHhMm"
  | "diary.finishedCoffeeConfirmText"
  | "diary.removePantryConfirmText"
  | "diary.totalCoffeeAmount"
  | "diary.remainingCoffeeAmount"
  | "diary.enterValidValues"
  | "diary.totalMustBeGreaterThanZero"
  | "diary.remainingNotNegative"
  | "diary.remainingNotExceedTotal"
  | "diary.enterValidAmount"
  | "diary.amountMustBeGreaterThanZero"
  | "diary.enterValidCaffeine"
  | "diary.caffeineCannotBeNegative"
  | "diary.enterValidDose"
  | "diary.quantityMl"
  | "diary.doseGrams"
  | "diary.hhmmPlaceholder"
  | "diary.entry"
  | "diary.estimatedCaffeineUpper"
  | "diary.estimatedCaffeineAria"
  | "diary.estimatedCaffeineTooltip"
  | "diary.hydrationUpper"
  | "diary.consumptionChartAria"
  | "diary.methodUpper"
  | "diary.type"
  | "diary.caffeineMg"
  | "diary.size"
  | "diary.noMethod"
  | "diary.quickLog"
  | "diary.coffeeBrandUpper"
  | "language.title"
  | "language.system"
  | "language.systemDescription"
  | "language.es"
  | "language.en"
  | "language.fr"
  | "language.pt"
  | "language.de"
  | "lists.privacy.public.label"
  | "lists.privacy.public.desc"
  | "lists.privacy.invitation.label"
  | "lists.privacy.invitation.desc"
  | "lists.privacy.private.label"
  | "lists.privacy.private.desc";

type Messages = Record<I18nKey, string>;
export type MessageTable = Record<Locale, Messages>;

export const MESSAGES: MessageTable = {
  es: {
    "common.reload": "Recargar",
    "common.back": "Volver",
    "common.cancel": "Cancelar",
    "common.save": "Guardar",
    "common.saving": "Guardando...",
    "common.loading": "Cargando...",
    "common.close": "Cerrar",
    "common.next": "Siguiente",
    "common.register": "Registrar",
    "root.errorTitle": "Se produjo un error",
    "root.renderError": "No se pudo renderizar la aplicación.",
    "nav.main": "Navegacion principal",
    "nav.home": "Inicio",
    "nav.search": "Explorar",
    "nav.brewlab": "Elabora",
    "nav.diary": "Diario",
    "nav.profile": "Perfil",
    "login.mainAria": "Inicio de sesión",
    "login.joinCommunity": "Únete a la comunidad del café para descubrir, elaborar y compartir tu pasión.",
    "login.signUpGoogle": "Registrarse con Google",
    "login.termsPrefix": "Al continuar, aceptas nuestra",
    "login.privacy": "Política de Privacidad",
    "login.terms": "Condiciones del Servicio",
    "login.dataDeletion": "Eliminación de datos",
    "login.welcome": "BIENVENIDO A",
    "login.subtitle": "La comunidad para los amantes del café.",
    "login.benefits": "Beneficios",
    "login.manageTitle": "Gestiona",
    "login.manageDesc": "Organiza tu café en casa y ten control total de tu despensa.",
    "login.exploreTitle": "Explora",
    "login.exploreDesc": "Descubre cafés, tostadores y perfiles que encajan contigo.",
    "login.brewTitle": "Elabora",
    "login.brewDesc": "Convierte cada preparación en una experiencia de barista en casa.",
    "login.trackTitle": "Registra",
    "login.trackDesc": "Guarda tus catas y descubre cómo evoluciona tu paladar.",
    "login.startNow": "EMPEZAR AHORA",
    "login.desktopAccess": "Acceso desktop",
    "login.logoAlt": "Logo Cafesito",
    "cookie.aria": "Aviso de cookies",
    "cookie.title": "Cookies",
    "cookie.description": "Utilizamos las cookies necesarias para garantizar el correcto funcionamiento de la aplicación y las cookies de análisis para mejorar tu experiencia. Puedes aceptar solo las necesarias o permitir todas.",
    "cookie.privacyMore": "Más información en la Política de Privacidad",
    "cookie.essentialOnly": "Solo esenciales",
    "cookie.acceptAll": "Aceptar todas",
    "google.connecting": "Conectando...",
    "notFound.aria": "Pagina no encontrada",
    "notFound.kicker": "ERROR 404",
    "notFound.title": "Se nos fue el espresso por otro filtro",
    "notFound.body": "Esta pagina no existe o cambio de taza. Tranquilo: la cafetera sigue caliente y te podemos llevar a algo mejor.",
    "notFound.goHome": "Volver al inicio",
    "notFound.randomCoffee": "Sorprendeme con un cafe",
    "top.search.back": "Atrás",
    "top.search.users.placeholder": "Buscar usuarios...",
    "top.search.users.aria": "Buscar usuarios",
    "top.search.aria": "Busqueda",
    "top.search.placeholder": "Busca cafe o marca",
    "top.search.scan": "Escanear código",
    "top.search.filtersAria": "Filtros de busqueda",
    "top.filter.country": "PAIS",
    "top.filter.specialty": "ESPECIALIDAD",
    "top.filter.roast": "TUESTE",
    "top.filter.format": "FORMATO",
    "top.filter.rating": "NOTA",
    "top.createCoffee": "Crea tu café",
    "top.selectCoffee": "Selecciona café",
    "top.saveLabel": "Guardar",
    "top.goBrewing": "Ir a proceso en curso",
    "top.goResult": "Ir a resultado",
    "top.nextConsumption": "Siguiente: ir a consumo",
    "top.prevMonth": "Mes anterior",
    "top.prev": "Anterior",
    "top.nextMonth": "Mes siguiente",
    "top.next": "Siguiente",
    "top.selectPeriod": "Seleccionar periodo",
    "top.myDiary": "MI DIARIO",
    "top.history": "HISTORIAL",
    "top.followers": "SEGUIDORES",
    "top.following": "SIGUIENDO",
    "top.favorites": "FAVORITOS",
    "top.listDefault": "Lista",
    "top.joinThisList": "Unirse a esta lista",
    "top.join": "Unirse",
    "top.listOptions": "Opciones de lista",
    "top.searchUsers": "Buscar usuarios",
    "top.profile": "PERFIL",
    "top.profileOptions": "Opciones de perfil",
    "top.general": "General",
    "top.account": "Cuenta",
    "top.coffeesConsumed": "Cafés consumidos",
    "top.editProfile": "Editar perfil",
    "top.language": "Idioma",
    "top.deleteAccountData": "Eliminar mi cuenta y mis datos",
    "top.signOut": "Cerrar sesión",
    "top.deleteAccountAria": "Eliminar cuenta",
    "top.deleteAccountText": "Tu cuenta quedará inactiva durante 30 días y luego se eliminará con todos tus datos. Si vuelves a acceder antes, se cancelará el proceso.",
    "top.processing": "Procesando...",
    "top.delete": "Eliminar",
    "coffee.notFound": "Café no encontrado",
    "account.reactivatedBanner": "Se canceló la eliminación de tu cuenta porque volviste a iniciar sesión.",
    "share.listTitle": "Lista de Cafesito",
    "share.listText": "Te comparto esta lista",
    "top.coffee": "CAFE",
    "top.removeFromLists": "Quitar de listas",
    "top.addToLists": "Añadir a listas",
    "top.addStock": "Añadir a stock",
    "top.notifications": "Notificaciones",
    "search.followersFollowing": "{followers} seguidores · {following} siguiendo",
    "search.following": "Siguiendo",
    "search.follow": "Seguir",
    "search.noUsersFound": "No se encontraron usuarios",
    "search.findFriends": "Busca amigos para seguir",
    "search.recentSearches": "Busquedas recientes",
    "search.clear": "Limpiar",
    "search.coffeeProfile": "PERFIL DE CAFE",
    "search.seeAll": "Ver todos",
    "search.brandFallback": "Marca",
    "search.noCoffeeWithFilters": "No encontramos cafes con esos filtros.",
    "search.filters": "Filtros",
    "search.clearFilters": "Limpiar filtros",
    "search.anyRating": "Cualquier nota",
    "search.minRating": "Nota minima: {value}+",
    "notifications.today": "Hoy",
    "notifications.yesterday": "Ayer",
    "notifications.last7": "Últimos 7 días",
    "notifications.last30": "Últimos 30 días",
    "notifications.title": "NOTIFICACIONES",
    "notifications.empty": "No tienes notificaciones",
    "notifications.inviteGroup": "Invitación a lista",
    "notifications.declineInvite": "Rechazar invitación a la lista",
    "notifications.addList": "Añadir",
    "notifications.following": "SIGUIENDO",
    "notifications.follow": "SEGUIR",
    "notifications.reply": "RESPONDER",
    "root.unexpectedError": "Error inesperado",
    "auth.access": "Acceso",
    "tour.skipAll": "Omitir todo",
    "tour.gotIt": "Entendido",
    "lists.join.aria": "Unirse a la lista",
    "lists.join.invited": "Te han invitado a una lista",
    "lists.join.ownerBy": "de @{owner}",
    "lists.join.joining": "Uniendo…",
    "lists.join.join": "Unirse a la lista",
    "lists.join.notFound": "Lista no encontrada",
    "lists.join.notFoundHint": "El enlace no es válido o la lista ya no permite unirse.",
    "lists.newList": "Nueva lista",
    "lists.createList": "Crear lista",
    "lists.creating": "Creando…",
    "lists.editList": "Editar lista",
    "lists.listName": "Nombre de la lista",
    "lists.listNamePlaceholder": "Ej: Para probar",
    "lists.privacy": "Privacidad",
    "lists.allowEdit": "Permitir editar lista",
    "lists.allowEditAria": "Permitir que los miembros añadan o quiten cafés de la lista",
    "lists.errorCreate": "Error al crear la lista",
    "lists.errorSave": "Error al guardar",
    "lists.general": "General",
    "lists.deleteList": "Eliminar lista",
    "lists.leaveList": "Salir de la lista",
    "lists.allowInvite": "Permitir que los miembros inviten",
    "lists.allowInviteAria": "Permitir que los miembros inviten a otras personas al grupo",
    "profile.noFollowers": "Sin seguidores",
    "profile.notFollowingAnyone": "No sigue a nadie",
    "scanner.aria": "Escanear codigo",
    "scanner.title": "ESCANEAR CODIGO",
    "scanner.httpsRequired": "La cámara en web requiere HTTPS.",
    "scanner.browserNoCamera": "Este navegador no permite acceso a cámara.",
    "scanner.permissionDenied": "No tenemos permiso para usar la cámara. Revísalo en el navegador.",
    "scanner.noCompatibleCamera": "No encontramos una cámara compatible en este dispositivo.",
    "scanner.openFailed": "No pudimos abrir la cámara. Cierra otras apps que la estén usando y vuelve a intentar.",
    "scanner.hint": "Coloca el código de barras dentro del recuadro",
    "timeline.brewWith": "Elaborar con {method}",
    "timeline.prev": "Anterior",
    "timeline.next": "Siguiente",
    "timeline.invalidValues": "Introduce valores válidos.",
    "timeline.totalGreaterThanZero": "El total debe ser mayor que 0.",
    "timeline.remainingNonNegative": "El restante no puede ser negativo.",
    "timeline.remainingNoMoreThanTotal": "El restante no puede superar el total.",
    "timeline.yourPantry": "TU DESPENSA",
    "timeline.options": "Opciones",
    "timeline.addCoffeeToPantry": "Añadir café a despensa",
    "timeline.dailyRecommendations": "Recomendaciones del día",
    "timeline.peopleToFollow": "Personas que podrias seguir",
    "timeline.followersOnly": "{count} seguidores",
    "timeline.pantryOptions": "Opciones despensa",
    "timeline.organize": "Organiza",
    "timeline.editStock": "Editar stock",
    "timeline.coffeeFinished": "Café terminado",
    "timeline.deleteFromPantry": "Eliminar de la despensa",
    "timeline.finishedCoffeeTitle": "Café terminado",
    "timeline.finishedCoffeeText": "¿Marcar este café como terminado? Se quitará de tu despensa y se guardará en Historial.",
    "timeline.confirm": "Confirmar",
    "timeline.deletingFromPantryTitle": "Eliminar de la despensa",
    "timeline.deletingFromPantryText": "¿Estás seguro de eliminar este café? Se borrará tu stock actual.",
    "timeline.deleting": "Eliminando...",
    "timeline.editStockTitle": "Editar Stock",
    "timeline.totalCoffeeGrams": "Cantidad de café total (g)",
    "timeline.remainingCoffeeGrams": "Cantidad de café restante (g)",
    "timeline.cancelUpper": "CANCELAR",
    "timeline.saveUpper": "GUARDAR",
    "timeline.savingUpper": "GUARDANDO...",
    "brew.method": "Método",
    "brew.methodAria": "Método de elaboración",
    "brew.selectCoffee": "Selecciona café",
    "brew.paramsMethod": "Parámetros del método",
    "brew.waterMl": "Agua (ml)",
    "brew.coffeeG": "Café (g)",
    "brew.timeS": "Tiempo (s)",
    "brew.baristaTips": "Consejos del barista",
    "brew.seeBaristaTips": "Ver consejos del barista",
    "brew.timer": "Temporizador",
    "brew.timerAria": "Temporizador",
    "brew.timerEnableAria": "Activar temporizador para proceso en curso",
    "brew.nextLabel": "Siguiente: {label}",
    "brew.finish": "Finalizar",
    "brew.total": "TOTAL {time}",
    "brew.restart": "REINICIAR",
    "brew.pause": "PAUSAR",
    "brew.start": "INICIAR",
    "brew.resultTasteTitle": "¿QUÉ SABOR HAS OBTENIDO?",
    "brew.recommendation": "Recomendación",
    "brew.configureCoffee": "Configura tu café",
    "brew.coffeeType": "Tipo de café",
    "brew.cupSize": "Tamaño de la taza",
    "brew.result": "Resultado",
    "brew.registerWater": "Registrar {ml} ml de agua",
    "brew.apply": "Aplicar",
    "coffee.back": "Volver",
    "coffee.closeDetail": "Cerrar detalle",
    "coffee.share": "Compartir café",
    "coffee.brandFallback": "Marca",
    "coffee.ratingAria": "Nota del café",
    "coffee.ratingTitle": "NOTA",
    "coffee.noDescription": "Sin descripción.",
    "coffee.technicalDetails": "Detalles técnicos",
    "coffee.sensoryProfile": "Perfil sensorial",
    "coffee.edit": "Editar",
    "coffee.sensoryBasedOn": "Basado en los comentarios de {count} usuarios. {count} son las personas que lo han editado.",
    "coffee.acquire": "Adquirir",
    "coffee.opinions": "Opiniones",
    "coffee.add": "+ AÑADIR",
    "coffee.yourReview": "Tu reseña",
    "coffee.reviewImageAlt": "Imagen reseña",
    "coffee.editSensoryProfile": "Editar perfil sensorial",
    "coffee.saveSensoryError": "No se pudo guardar el perfil sensorial.",
    "coffee.saveStockError": "No se pudo guardar el stock.",
    "coffee.reviewSheetAria": "Tu opinión",
    "coffee.reviewTitle": "TU OPINIÓN",
    "coffee.selectRating": "Seleccionar nota",
    "coffee.reviewPlaceholder": "Escribe tu reseña",
    "coffee.attachImage": "Adjuntar imagen",
    "coffee.reviewPreviewAlt": "Previsualización reseña",
    "coffee.removeImage": "Quitar imagen",
    "coffee.delete": "Eliminar",
    "coffee.deleting": "Borrando...",
    "coffee.publish": "Publicar",
    "coffee.publishing": "Publicando...",
    "coffee.selectRatingError": "Selecciona una nota para guardar.",
    "coffee.writeReviewError": "Escribe tu reseña antes de publicar.",
    "coffee.addToList": "Añadir a lista",
    "coffee.createList": "Crear una lista",
    "coffee.favorites": "Favoritos",
    "coffee.applyChanges": "Aplicar",
    "coffee.applying": "Aplicando…",
    "coffee.addOrRemoveFromList": "{action} {name}",
    "diary.selectPeriod": "SELECCIONAR PERIODO",
    "diary.periodToday": "HOY",
    "diary.periodWeek": "SEMANA",
    "diary.periodMonth": "MES",
    "diary.prevMonth": "Mes anterior",
    "diary.nextMonth": "Mes siguiente",
    "diary.select": "Selecciona",
    "diary.goToday": "Ir a hoy",
    "diary.logWater": "Registrar agua",
    "diary.water": "Agua",
    "diary.log": "Registrar",
    "diary.amountMl": "Cantidad en ml",
    "diary.selectCoffee": "Seleccionar café",
    "diary.yourPantry": "TU DESPENSA",
    "diary.pantryEmpty": "Tu despensa está vacía",
    "diary.searchCoffeeBrand": "Busca un café o marca",
    "diary.searchCoffeeBrandAria": "Buscar café o marca",
    "diary.scanBarcode": "Escanear código de barras",
    "diary.suggestions": "SUGERENCIAS",
    "diary.createMyCoffee": "Crear mi café",
    "diary.cancel": "Cancelar",
    "diary.close": "Cerrar",
    "profile.changePhoto": "Cambiar foto",
    "profile.name": "Nombre",
    "profile.bio": "Bio",
    "profile.following": "Siguiendo",
    "profile.follow": "Seguir",
    "profile.saving": "Guardando...",
    "profile.save": "Guardar",
    "profile.stats": "Estadisticas de perfil",
    "profile.seguidores": "SEGUIDORES",
    "profile.tabProfile": "Tabs perfil",
    "profile.tabActivity": "Actividad",
    "profile.tabAdn": "ADN",
    "profile.tabLists": "Listas",
    "profile.activityOfThisUser": "Actividad de este usuario",
    "profile.activityMineAndFollowing": "Tu actividad y la de personas que sigues",
    "profile.activity.review.other": "opinó sobre un café",
    "profile.activity.review.own": "opinaste sobre un café",
    "profile.activity.diary.other": "probó por primera vez",
    "profile.activity.diary.own": "probaste por primera vez",
    "profile.activity.favorite.other": "añadió a su lista",
    "profile.activity.favorite.own": "añadiste a tu lista",
    "profile.activity.listFallback": "Lista",
    "profile.activity.viewListAria": "Ver lista {listName}",
    "profile.activity.ratingAndReviewAria": "Valoración y opinión",
    "profile.activity.coffeeImageAlt": "Imagen de {coffeeName}",
    "profile.preferenceAnalysis": "Análisis de preferencia",
    "diary.habit": "Hábito",
    "diary.consumption": "Consumo",
    "diary.barista": "Barista",
    "diary.activity": "Actividad",
    "diary.cups": "Tazas",
    "diary.cupSize": "Tamaño tazas",
    "diary.method": "Método",
    "diary.coffeeDay": "Día cafetero",
    "diary.moment": "Momento",
    "diary.morning": "Mañana",
    "diary.afternoon": "Tarde",
    "diary.night": "Noche",
    "diary.caffeine": "Cafeína",
    "diary.dosePerCoffee": "Dosis por café",
    "diary.format": "Formato",
    "diary.pantryForecast": "Previsión despensa",
    "diary.daysApprox": "~{days} días",
    "diary.coffeesTried": "Cafés probados",
    "diary.viewCoffeesTried": "Ver listado de cafés probados",
    "diary.roastersTried": "Tostadores probados",
    "diary.favoriteOrigin": "Origen favorito",
    "diary.firstTime": "Primera vez: {date}",
    "diary.pantryOptions": "Opciones despensa",
    "diary.organize": "Organiza",
    "diary.general": "General",
    "diary.editStock": "Editar stock",
    "diary.coffeeFinished": "Café terminado",
    "diary.removeFromPantry": "Eliminar de la despensa",
    "diary.confirm": "Confirmar",
    "diary.removing": "Eliminando...",
    "diary.delete": "Eliminar",
    "diary.editEntry": "Editar entrada",
    "diary.edit": "Editar",
    "diary.timeHhMm": "Tiempo (hh:mm)",
    "diary.finishedCoffeeConfirmText": "¿Marcar este café como terminado? Se quitará de tu despensa y se guardará en Historial.",
    "diary.removePantryConfirmText": "¿Estás seguro de eliminar este café? Se borrará tu stock actual.",
    "diary.totalCoffeeAmount": "Cantidad de café total (g)",
    "diary.remainingCoffeeAmount": "Cantidad de café restante (g)",
    "diary.enterValidValues": "Introduce valores válidos.",
    "diary.totalMustBeGreaterThanZero": "El total debe ser mayor que 0.",
    "diary.remainingNotNegative": "El restante no puede ser negativo.",
    "diary.remainingNotExceedTotal": "El restante no puede superar el total.",
    "diary.enterValidAmount": "Introduce una cantidad válida.",
    "diary.amountMustBeGreaterThanZero": "La cantidad debe ser mayor que 0 ml.",
    "diary.enterValidCaffeine": "Introduce una cafeína válida.",
    "diary.caffeineCannotBeNegative": "La cafeína no puede ser negativa.",
    "diary.enterValidDose": "Introduce una dosis válida.",
    "diary.quantityMl": "Cantidad (ml)",
    "diary.doseGrams": "Dosis (g)",
    "diary.hhmmPlaceholder": "HH:mm",
    "diary.entry": "Entrada",
    "diary.estimatedCaffeineUpper": "CAFEINA ESTIMADA",
    "diary.estimatedCaffeineAria": "Información de cafeína estimada",
    "diary.estimatedCaffeineTooltip": "Estimación basada en tus registros de consumo en el periodo seleccionado.",
    "diary.hydrationUpper": "HIDRATACION",
    "diary.consumptionChartAria": "Gráfico de consumo",
    "diary.methodUpper": "METODO",
    "diary.type": "Tipo",
    "diary.caffeineMg": "Cafeína (mg)",
    "diary.size": "Tamaño",
    "diary.noMethod": "Sin método",
    "diary.quickLog": "Registro rápido",
    "diary.coffeeBrandUpper": "CAFE",
    "language.title": "Idioma",
    "language.system": "Sistema",
    "language.systemDescription": "Usa el idioma del sistema. Si no está disponible, se aplica inglés.",
    "language.es": "Español",
    "language.en": "English",
    "language.fr": "Français",
    "language.pt": "Português",
    "language.de": "Deutsch",
    "lists.privacy.public.label": "Pública",
    "lists.privacy.public.desc": "Cualquier persona puede suscribirse. Visible en actividad.",
    "lists.privacy.invitation.label": "Por invitación",
    "lists.privacy.invitation.desc": "Solo quienes invites podrán ver la lista. No visible en actividad.",
    "lists.privacy.private.label": "Privada",
    "lists.privacy.private.desc": "Solo tú. No visible en actividad."
  },
  en: {} as Messages,
  fr: {} as Messages,
  pt: {} as Messages,
  de: {} as Messages
};

const en: Messages = {
  ...MESSAGES.es,
  "common.reload": "Reload",
  "common.back": "Back",
  "common.cancel": "Cancel",
  "common.save": "Save",
  "common.saving": "Saving...",
  "common.loading": "Loading...",
  "common.close": "Close",
  "common.next": "Next",
  "common.register": "Register",
  "root.errorTitle": "An error occurred",
  "root.unexpectedError": "Unexpected error",
  "root.renderError": "The application could not be rendered.",
  "nav.main": "Main navigation",
  "nav.home": "Home",
  "nav.search": "Explore",
  "nav.brewlab": "Brew",
  "nav.diary": "Diary",
  "nav.profile": "Profile",
  "login.mainAria": "Sign in",
  "login.joinCommunity": "Join the coffee community to discover, brew, and share your passion.",
  "login.signUpGoogle": "Sign up with Google",
  "login.termsPrefix": "By continuing, you accept our",
  "login.privacy": "Privacy Policy",
  "login.terms": "Terms of Service",
  "login.dataDeletion": "Data deletion",
  "login.welcome": "WELCOME TO",
  "login.subtitle": "The community for coffee lovers.",
  "login.benefits": "Benefits",
  "login.manageTitle": "Manage",
  "login.manageDesc": "Organize your coffee at home and keep full control of your pantry.",
  "login.exploreTitle": "Explore",
  "login.exploreDesc": "Discover coffees, roasters, and profiles that match your taste.",
  "login.brewTitle": "Brew",
  "login.brewDesc": "Turn every preparation into an at-home barista experience.",
  "login.trackTitle": "Track",
  "login.trackDesc": "Save your tastings and discover how your palate evolves.",
  "login.startNow": "GET STARTED",
  "login.desktopAccess": "Desktop access",
  "login.logoAlt": "Cafesito logo",
  "cookie.aria": "Cookie notice",
  "cookie.privacyMore": "More information in the Privacy Policy",
  "cookie.essentialOnly": "Essential only",
  "cookie.acceptAll": "Accept all",
  "google.connecting": "Connecting...",
  "notFound.aria": "Page not found",
  "notFound.title": "Our espresso took another route",
  "notFound.body": "This page does not exist or changed cup. Relax: the coffee machine is still warm and we can take you somewhere better.",
  "notFound.goHome": "Back to home",
  "notFound.randomCoffee": "Surprise me with a coffee",
  "top.search.users.placeholder": "Search users...",
  "top.search.users.aria": "Search users",
  "top.search.aria": "Search",
  "top.search.placeholder": "Search coffee or brand",
  "top.search.scan": "Scan barcode",
  "top.filter.country": "COUNTRY",
  "top.filter.specialty": "SPECIALTY",
  "top.filter.roast": "ROAST",
  "top.filter.format": "FORMAT",
  "top.filter.rating": "RATING",
  "top.createCoffee": "Create your coffee",
  "top.selectCoffee": "Select coffee",
  "top.myDiary": "MY DIARY",
  "top.history": "HISTORY",
  "top.followers": "FOLLOWERS",
  "top.following": "FOLLOWING",
  "top.favorites": "FAVORITES",
  "top.joinThisList": "Join this list",
  "top.join": "Join",
  "top.searchUsers": "Search users",
  "top.profile": "PROFILE",
  "top.general": "General",
  "top.account": "Account",
  "top.coffeesConsumed": "Coffees consumed",
  "top.editProfile": "Edit profile",
  "top.language": "Language",
  "top.deleteAccountData": "Delete my account and data",
  "top.signOut": "Sign out",
  "top.deleteAccountAria": "Delete account",
  "top.deleteAccountText": "Your account will remain inactive for 30 days and then be deleted with all your data. If you sign in again before that, the process will be canceled.",
  "top.processing": "Processing...",
  "top.delete": "Delete",
  "coffee.notFound": "Coffee not found",
  "account.reactivatedBanner": "Your account deletion was canceled because you signed in again.",
  "share.listTitle": "Cafesito list",
  "share.listText": "I am sharing this list with you",
  "top.removeFromLists": "Remove from lists",
  "top.addToLists": "Add to lists",
  "top.addStock": "Add to stock",
  "top.notifications": "Notifications",
  "search.followersFollowing": "{followers} followers · {following} following",
  "search.following": "Following",
  "search.follow": "Follow",
  "search.noUsersFound": "No users found",
  "search.findFriends": "Find friends to follow",
  "search.recentSearches": "Recent searches",
  "search.clear": "Clear",
  "search.coffeeProfile": "COFFEE PROFILE",
  "search.seeAll": "See all",
  "search.brandFallback": "Brand",
  "search.noCoffeeWithFilters": "No coffees found with those filters.",
  "search.filters": "Filters",
  "search.clearFilters": "Clear filters",
  "search.anyRating": "Any rating",
  "search.minRating": "Minimum rating: {value}+",
  "notifications.today": "Today",
  "notifications.yesterday": "Yesterday",
  "notifications.last7": "Last 7 days",
  "notifications.last30": "Last 30 days",
  "notifications.title": "NOTIFICATIONS",
  "notifications.empty": "You have no notifications",
  "notifications.inviteGroup": "List invitation",
  "notifications.declineInvite": "Decline list invitation",
  "notifications.addList": "Add",
  "notifications.following": "FOLLOWING",
  "notifications.follow": "FOLLOW",
  "notifications.reply": "REPLY",
  "auth.access": "Access",
  "tour.skipAll": "Skip all",
  "tour.gotIt": "Got it",
  "lists.join.aria": "Join list",
  "lists.join.invited": "You were invited to a list",
  "lists.join.ownerBy": "by @{owner}",
  "lists.join.joining": "Joining…",
  "lists.join.join": "Join list",
  "lists.join.notFound": "List not found",
  "lists.join.notFoundHint": "The link is invalid or the list no longer accepts joins.",
  "lists.newList": "New list",
  "lists.createList": "Create list",
  "lists.creating": "Creating…",
  "lists.editList": "Edit list",
  "lists.listName": "List name",
  "lists.listNamePlaceholder": "Ex: To try",
  "lists.privacy": "Privacy",
  "lists.allowEdit": "Allow list editing",
  "lists.allowEditAria": "Allow members to add or remove coffees from the list",
  "lists.errorCreate": "Error creating list",
  "lists.errorSave": "Error saving",
  "lists.general": "General",
  "lists.deleteList": "Delete list",
  "lists.leaveList": "Leave list",
  "lists.allowInvite": "Allow members to invite",
  "lists.allowInviteAria": "Allow members to invite other people to the group",
  "profile.noFollowers": "No followers",
  "profile.notFollowingAnyone": "Not following anyone",
  "scanner.aria": "Scan barcode",
  "scanner.title": "SCAN BARCODE",
  "scanner.httpsRequired": "Camera on web requires HTTPS.",
  "scanner.browserNoCamera": "This browser does not allow camera access.",
  "scanner.permissionDenied": "Camera permission is not granted. Please review browser permissions.",
  "scanner.noCompatibleCamera": "No compatible camera found on this device.",
  "scanner.openFailed": "Could not open the camera. Close other apps using it and try again.",
  "scanner.hint": "Place the barcode inside the frame",
  "timeline.brewWith": "Brew with {method}",
  "timeline.prev": "Previous",
  "timeline.next": "Next",
  "timeline.invalidValues": "Enter valid values.",
  "timeline.totalGreaterThanZero": "Total must be greater than 0.",
  "timeline.remainingNonNegative": "Remaining cannot be negative.",
  "timeline.remainingNoMoreThanTotal": "Remaining cannot exceed total.",
  "timeline.yourPantry": "YOUR PANTRY",
  "timeline.options": "Options",
  "timeline.addCoffeeToPantry": "Add coffee to pantry",
  "timeline.dailyRecommendations": "Daily recommendations",
  "timeline.peopleToFollow": "People you may follow",
  "timeline.followersOnly": "{count} followers",
  "timeline.pantryOptions": "Pantry options",
  "timeline.organize": "Organize",
  "timeline.editStock": "Edit stock",
  "timeline.coffeeFinished": "Coffee finished",
  "timeline.deleteFromPantry": "Remove from pantry",
  "timeline.finishedCoffeeTitle": "Coffee finished",
  "timeline.finishedCoffeeText": "Mark this coffee as finished? It will be removed from your pantry and saved to History.",
  "timeline.confirm": "Confirm",
  "timeline.deletingFromPantryTitle": "Remove from pantry",
  "timeline.deletingFromPantryText": "Are you sure you want to remove this coffee? Your current stock will be deleted.",
  "timeline.deleting": "Deleting...",
  "timeline.editStockTitle": "Edit Stock",
  "timeline.totalCoffeeGrams": "Total coffee amount (g)",
  "timeline.remainingCoffeeGrams": "Remaining coffee amount (g)",
  "timeline.cancelUpper": "CANCEL",
  "timeline.saveUpper": "SAVE",
  "timeline.savingUpper": "SAVING...",
  "brew.method": "Method",
  "brew.methodAria": "Brewing method",
  "brew.selectCoffee": "Select coffee",
  "brew.paramsMethod": "Method parameters",
  "brew.waterMl": "Water (ml)",
  "brew.coffeeG": "Coffee (g)",
  "brew.timeS": "Time (s)",
  "brew.baristaTips": "Barista tips",
  "brew.seeBaristaTips": "View barista tips",
  "brew.timer": "Timer",
  "brew.timerAria": "Timer",
  "brew.timerEnableAria": "Enable timer for brewing process",
  "brew.nextLabel": "Next: {label}",
  "brew.finish": "Finish",
  "brew.total": "TOTAL {time}",
  "brew.restart": "RESTART",
  "brew.pause": "PAUSE",
  "brew.start": "START",
  "brew.resultTasteTitle": "WHAT FLAVOR DID YOU GET?",
  "brew.recommendation": "Recommendation",
  "brew.configureCoffee": "Configure your coffee",
  "brew.coffeeType": "Coffee type",
  "brew.cupSize": "Cup size",
  "brew.result": "Result",
  "brew.registerWater": "Log {ml} ml of water",
  "brew.apply": "Apply",
  "coffee.back": "Back",
  "coffee.closeDetail": "Close detail",
  "coffee.share": "Share coffee",
  "coffee.brandFallback": "Brand",
  "coffee.ratingAria": "Coffee rating",
  "coffee.ratingTitle": "RATING",
  "coffee.noDescription": "No description.",
  "coffee.technicalDetails": "Technical details",
  "coffee.sensoryProfile": "Sensory profile",
  "coffee.edit": "Edit",
  "coffee.sensoryBasedOn": "Based on comments from {count} users. {count} users have edited it.",
  "coffee.acquire": "Buy",
  "coffee.opinions": "Reviews",
  "coffee.add": "+ ADD",
  "coffee.yourReview": "Your review",
  "coffee.reviewImageAlt": "Review image",
  "coffee.editSensoryProfile": "Edit sensory profile",
  "coffee.saveSensoryError": "Could not save sensory profile.",
  "coffee.saveStockError": "Could not save stock.",
  "coffee.reviewSheetAria": "Your opinion",
  "coffee.reviewTitle": "YOUR REVIEW",
  "coffee.selectRating": "Select rating",
  "coffee.reviewPlaceholder": "Write your review",
  "coffee.attachImage": "Attach image",
  "coffee.reviewPreviewAlt": "Review preview",
  "coffee.removeImage": "Remove image",
  "coffee.delete": "Delete",
  "coffee.deleting": "Deleting...",
  "coffee.publish": "Publish",
  "coffee.publishing": "Publishing...",
  "coffee.selectRatingError": "Select a rating to save.",
  "coffee.writeReviewError": "Write your review before publishing.",
  "coffee.addToList": "Add to list",
  "coffee.createList": "Create a list",
  "coffee.favorites": "Favorites",
  "coffee.applyChanges": "Apply",
  "coffee.applying": "Applying…",
  "coffee.addOrRemoveFromList": "{action} {name}",
  "diary.selectPeriod": "SELECT PERIOD",
  "diary.periodToday": "TODAY",
  "diary.periodWeek": "WEEK",
  "diary.periodMonth": "MONTH",
  "diary.prevMonth": "Previous month",
  "diary.nextMonth": "Next month",
  "diary.select": "Select",
  "diary.goToday": "Go to today",
  "diary.logWater": "Log water",
  "diary.water": "Water",
  "diary.log": "Log",
  "diary.amountMl": "Amount in ml",
  "diary.selectCoffee": "Select coffee",
  "diary.yourPantry": "YOUR PANTRY",
  "diary.pantryEmpty": "Your pantry is empty",
  "diary.searchCoffeeBrand": "Search coffee or brand",
  "diary.searchCoffeeBrandAria": "Search coffee or brand",
  "diary.scanBarcode": "Scan barcode",
  "diary.suggestions": "SUGGESTIONS",
  "diary.createMyCoffee": "Create my coffee",
  "diary.cancel": "Cancel",
  "diary.close": "Close",
  "profile.changePhoto": "Change photo",
  "profile.name": "Name",
  "profile.bio": "Bio",
  "profile.following": "Following",
  "profile.follow": "Follow",
  "profile.saving": "Saving...",
  "profile.save": "Save",
  "profile.stats": "Profile statistics",
  "profile.seguidores": "FOLLOWERS",
  "profile.tabProfile": "Profile tabs",
  "profile.tabActivity": "Activity",
  "profile.tabAdn": "DNA",
  "profile.tabLists": "Lists",
  "profile.activityOfThisUser": "This user's activity",
  "profile.activityMineAndFollowing": "Your activity and people you follow",
  "profile.activity.review.other": "reviewed a coffee",
  "profile.activity.review.own": "reviewed a coffee",
  "profile.activity.diary.other": "tried for the first time",
  "profile.activity.diary.own": "tried for the first time",
  "profile.activity.favorite.other": "added to their list",
  "profile.activity.favorite.own": "added to your list",
  "profile.activity.listFallback": "List",
  "profile.activity.viewListAria": "Open list {listName}",
  "profile.activity.ratingAndReviewAria": "Rating and review",
  "profile.activity.coffeeImageAlt": "Image of {coffeeName}",
  "profile.preferenceAnalysis": "Preference analysis",
  "diary.habit": "Habit",
  "diary.consumption": "Consumption",
  "diary.barista": "Barista",
  "diary.activity": "Activity",
  "diary.cups": "Cups",
  "diary.cupSize": "Cup size",
  "diary.method": "Method",
  "diary.coffeeDay": "Coffee day",
  "diary.moment": "Moment",
  "diary.morning": "Morning",
  "diary.afternoon": "Afternoon",
  "diary.night": "Night",
  "diary.caffeine": "Caffeine",
  "diary.dosePerCoffee": "Dose per coffee",
  "diary.format": "Format",
  "diary.pantryForecast": "Pantry forecast",
  "diary.daysApprox": "~{days} days",
  "diary.coffeesTried": "Coffees tried",
  "diary.viewCoffeesTried": "View coffees tried list",
  "diary.roastersTried": "Roasters tried",
  "diary.favoriteOrigin": "Favorite origin",
  "diary.firstTime": "First time: {date}",
  "diary.pantryOptions": "Pantry options",
  "diary.organize": "Organize",
  "diary.general": "General",
  "diary.editStock": "Edit stock",
  "diary.coffeeFinished": "Coffee finished",
  "diary.removeFromPantry": "Remove from pantry",
  "diary.confirm": "Confirm",
  "diary.removing": "Removing...",
  "diary.delete": "Delete",
  "diary.editEntry": "Edit entry",
  "diary.edit": "Edit",
  "diary.timeHhMm": "Time (hh:mm)",
  "diary.finishedCoffeeConfirmText": "Mark this coffee as finished? It will be removed from your pantry and saved in History.",
  "diary.removePantryConfirmText": "Are you sure you want to remove this coffee? Your current stock will be deleted.",
  "diary.totalCoffeeAmount": "Total coffee amount (g)",
  "diary.remainingCoffeeAmount": "Remaining coffee amount (g)",
  "diary.enterValidValues": "Enter valid values.",
  "diary.totalMustBeGreaterThanZero": "Total must be greater than 0.",
  "diary.remainingNotNegative": "Remaining cannot be negative.",
  "diary.remainingNotExceedTotal": "Remaining cannot exceed total.",
  "diary.enterValidAmount": "Enter a valid amount.",
  "diary.amountMustBeGreaterThanZero": "Amount must be greater than 0 ml.",
  "diary.enterValidCaffeine": "Enter valid caffeine.",
  "diary.caffeineCannotBeNegative": "Caffeine cannot be negative.",
  "diary.enterValidDose": "Enter a valid dose.",
  "diary.quantityMl": "Amount (ml)",
  "diary.doseGrams": "Dose (g)",
  "diary.hhmmPlaceholder": "HH:mm",
  "diary.entry": "Entry",
  "diary.estimatedCaffeineUpper": "ESTIMATED CAFFEINE",
  "diary.estimatedCaffeineAria": "Estimated caffeine info",
  "diary.estimatedCaffeineTooltip": "Estimate based on your consumption records in the selected period.",
  "diary.hydrationUpper": "HYDRATION",
  "diary.consumptionChartAria": "Consumption chart",
  "diary.methodUpper": "METHOD",
  "diary.type": "Type",
  "diary.caffeineMg": "Caffeine (mg)",
  "diary.size": "Size",
  "diary.noMethod": "No method",
  "diary.quickLog": "Quick log",
  "diary.coffeeBrandUpper": "COFFEE",
  "language.title": "Language",
  "language.system": "System",
  "language.systemDescription": "Use the system language. If unavailable, English is applied.",
  "language.es": "Español",
  "language.en": "English",
  "language.fr": "Français",
  "language.pt": "Português",
  "language.de": "Deutsch",
  "lists.privacy.public.label": "Public",
  "lists.privacy.public.desc": "Anyone can subscribe. Visible in activity.",
  "lists.privacy.invitation.label": "By invitation",
  "lists.privacy.invitation.desc": "Only invited users can see the list. Not visible in activity.",
  "lists.privacy.private.label": "Private",
  "lists.privacy.private.desc": "Only you. Not visible in activity."
};

const fr: Messages = {
  ...en,
  "nav.home": "Accueil",
  "nav.profile": "Profil",
  "login.mainAria": "Connexion",
  "login.joinCommunity": "Rejoins la communaute cafe pour decouvrir, preparer et partager ta passion.",
  "login.signUpGoogle": "S'inscrire avec Google",
  "login.termsPrefix": "En continuant, tu acceptes notre",
  "login.privacy": "Politique de confidentialite",
  "login.terms": "Conditions d'utilisation",
  "login.dataDeletion": "Suppression des donnees",
  "login.welcome": "BIENVENUE SUR",
  "login.subtitle": "La communaute pour les passionnes de cafe.",
  "login.benefits": "Avantages",
  "login.manageTitle": "Gere",
  "login.manageDesc": "Organise ton cafe a la maison et garde le controle total de ton stock.",
  "login.exploreTitle": "Explore",
  "login.exploreDesc": "Decouvre des cafes, des torrefacteurs et des profils qui te correspondent.",
  "login.brewTitle": "Prepare",
  "login.brewDesc": "Transforme chaque preparation en experience barista a la maison.",
  "login.trackTitle": "Suis",
  "login.trackDesc": "Enregistre tes degustations et observe l'evolution de ton palais.",
  "login.startNow": "COMMENCER",
  "login.desktopAccess": "Acces desktop",
  "login.logoAlt": "Logo Cafesito",
  "top.search.placeholder": "Rechercher un cafe ou une marque",
  "top.filter.country": "PAYS",
  "top.filter.specialty": "SPECIALITE",
  "top.filter.roast": "TORREFACTION",
  "top.filter.format": "FORMAT",
  "top.filter.rating": "NOTE",
  "top.createCoffee": "Cree ton cafe",
  "top.selectCoffee": "Selectionner un cafe",
  "brew.method": "Methode",
  "brew.selectCoffee": "Selectionner un cafe",
  "brew.baristaTips": "Conseils du barista",
  "brew.seeBaristaTips": "Voir les conseils du barista",
  "brew.timer": "Minuteur",
  "brew.result": "Resultat",
  "brew.coffeeType": "Type de cafe",
  "profile.tabActivity": "Activite",
  "profile.tabAdn": "ADN",
  "profile.tabLists": "Listes",
  "profile.activity.review.other": "a donne son avis sur un cafe",
  "profile.activity.review.own": "as donne ton avis sur un cafe",
  "profile.activity.diary.other": "a essaye pour la premiere fois",
  "profile.activity.diary.own": "as essaye pour la premiere fois",
  "profile.activity.favorite.other": "a ajoute a sa liste",
  "profile.activity.favorite.own": "as ajoute a ta liste",
  "profile.activity.listFallback": "Liste",
  "profile.activity.viewListAria": "Voir la liste {listName}",
  "profile.activity.ratingAndReviewAria": "Note et avis",
  "profile.activity.coffeeImageAlt": "Image de {coffeeName}"
};
const pt: Messages = {
  ...en,
  "nav.home": "Inicio",
  "nav.profile": "Perfil",
  "login.mainAria": "Entrar",
  "login.joinCommunity": "Junte-se a comunidade do cafe para descobrir, preparar e partilhar a sua paixao.",
  "login.signUpGoogle": "Registrar com Google",
  "login.termsPrefix": "Ao continuar, voce aceita nossa",
  "login.privacy": "Politica de Privacidade",
  "login.terms": "Termos de Servico",
  "login.dataDeletion": "Eliminacao de dados",
  "login.welcome": "BEM-VINDO AO",
  "login.subtitle": "A comunidade para amantes de cafe.",
  "login.benefits": "Beneficios",
  "login.manageTitle": "Gerencie",
  "login.manageDesc": "Organize seu cafe em casa e tenha controle total da sua despensa.",
  "login.exploreTitle": "Explore",
  "login.exploreDesc": "Descubra cafes, torrefadores e perfis que combinam com voce.",
  "login.brewTitle": "Prepare",
  "login.brewDesc": "Transforme cada preparo em uma experiencia de barista em casa.",
  "login.trackTitle": "Registre",
  "login.trackDesc": "Guarde suas degustacoes e descubra como seu paladar evolui.",
  "login.startNow": "COMECE AGORA",
  "login.desktopAccess": "Acesso desktop",
  "login.logoAlt": "Logo Cafesito",
  "top.search.placeholder": "Buscar cafe ou marca",
  "top.filter.country": "PAIS",
  "top.filter.specialty": "ESPECIALIDADE",
  "top.filter.roast": "TORRA",
  "top.filter.format": "FORMATO",
  "top.filter.rating": "NOTA",
  "top.createCoffee": "Crie seu cafe",
  "top.selectCoffee": "Selecionar cafe",
  "brew.method": "Metodo",
  "brew.selectCoffee": "Selecionar cafe",
  "brew.baristaTips": "Dicas do barista",
  "brew.seeBaristaTips": "Ver dicas do barista",
  "brew.timer": "Temporizador",
  "brew.result": "Resultado",
  "brew.coffeeType": "Tipo de cafe",
  "profile.tabActivity": "Atividade",
  "profile.tabAdn": "DNA",
  "profile.tabLists": "Listas",
  "profile.activity.review.other": "avaliou um cafe",
  "profile.activity.review.own": "avaliou um cafe",
  "profile.activity.diary.other": "provou pela primeira vez",
  "profile.activity.diary.own": "provou pela primeira vez",
  "profile.activity.favorite.other": "adicionou a sua lista",
  "profile.activity.favorite.own": "adicionou a sua lista",
  "profile.activity.listFallback": "Lista",
  "profile.activity.viewListAria": "Ver lista {listName}",
  "profile.activity.ratingAndReviewAria": "Avaliacao e opiniao",
  "profile.activity.coffeeImageAlt": "Imagem de {coffeeName}"
};
const de: Messages = {
  ...en,
  "nav.home": "Start",
  "nav.profile": "Profil",
  "login.mainAria": "Anmeldung",
  "login.joinCommunity": "Tritt der Kaffee-Community bei, um zu entdecken, zu bruehen und deine Leidenschaft zu teilen.",
  "login.signUpGoogle": "Mit Google registrieren",
  "login.termsPrefix": "Mit dem Fortfahren akzeptierst du unsere",
  "login.privacy": "Datenschutzrichtlinie",
  "login.terms": "Nutzungsbedingungen",
  "login.dataDeletion": "Datenloeschung",
  "login.welcome": "WILLKOMMEN BEI",
  "login.subtitle": "Die Community fuer Kaffeeliebhaber.",
  "login.benefits": "Vorteile",
  "login.manageTitle": "Verwalte",
  "login.manageDesc": "Organisiere deinen Kaffee zu Hause und behalte die volle Kontrolle ueber deine Vorratskammer.",
  "login.exploreTitle": "Entdecke",
  "login.exploreDesc": "Entdecke Kaffees, Roester und Profile, die zu dir passen.",
  "login.brewTitle": "Bruehe",
  "login.brewDesc": "Mach jede Zubereitung zu einem Barista-Erlebnis zu Hause.",
  "login.trackTitle": "Erfasse",
  "login.trackDesc": "Speichere deine Verkostungen und sieh, wie sich dein Geschmack entwickelt.",
  "login.startNow": "JETZT STARTEN",
  "login.desktopAccess": "Desktop-Zugang",
  "login.logoAlt": "Cafesito Logo",
  "top.search.placeholder": "Kaffee oder Marke suchen",
  "top.filter.country": "LAND",
  "top.filter.specialty": "SPEZIALITAT",
  "top.filter.roast": "ROSTUNG",
  "top.filter.format": "FORMAT",
  "top.filter.rating": "BEWERTUNG",
  "top.createCoffee": "Erstelle deinen Kaffee",
  "top.selectCoffee": "Kaffee auswahlen",
  "brew.method": "Methode",
  "brew.selectCoffee": "Kaffee auswahlen",
  "brew.baristaTips": "Barista-Tipps",
  "brew.seeBaristaTips": "Barista-Tipps anzeigen",
  "brew.timer": "Timer",
  "brew.result": "Ergebnis",
  "brew.coffeeType": "Kaffeeart",
  "profile.tabActivity": "Aktivitat",
  "profile.tabAdn": "DNA",
  "profile.tabLists": "Listen",
  "profile.activity.review.other": "hat einen Kaffee bewertet",
  "profile.activity.review.own": "hast einen Kaffee bewertet",
  "profile.activity.diary.other": "hat zum ersten Mal probiert",
  "profile.activity.diary.own": "hast zum ersten Mal probiert",
  "profile.activity.favorite.other": "hat zur Liste hinzugefugt",
  "profile.activity.favorite.own": "hast zu deiner Liste hinzugefugt",
  "profile.activity.listFallback": "Liste",
  "profile.activity.viewListAria": "Liste {listName} ansehen",
  "profile.activity.ratingAndReviewAria": "Bewertung und Rezension",
  "profile.activity.coffeeImageAlt": "Bild von {coffeeName}"
};

MESSAGES.en = en;
MESSAGES.fr = fr;
MESSAGES.pt = pt;
MESSAGES.de = de;
