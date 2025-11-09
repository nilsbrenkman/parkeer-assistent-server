package nl.parkeerassistent.html
//
//import kotlinx.css.Color
//import kotlinx.css.CssBuilder
//import kotlinx.css.LinearDimension
//import kotlinx.css.Margin
//import kotlinx.css.Padding
//import kotlinx.css.backgroundColor
//import kotlinx.css.body
//import kotlinx.css.borderWidth
//import kotlinx.css.boxShadow
//import kotlinx.css.height
//import kotlinx.css.margin
//import kotlinx.css.marginLeft
//import kotlinx.css.marginRight
//import kotlinx.css.maxWidth
//import kotlinx.css.padding
//import kotlinx.css.pct
//import kotlinx.css.properties.BoxShadow
//import kotlinx.css.px
//import kotlinx.css.rem
//import kotlinx.css.vh
//import kotlinx.css.width
//import kotlinx.html.HTML
//import kotlinx.html.body
//import kotlinx.html.head
//import kotlinx.html.id
//import kotlinx.html.iframe
//import kotlinx.html.img
//import kotlinx.html.meta
//import kotlinx.html.style
//import kotlinx.html.title
//
//private var Style = CssBuilder().apply {
//    body {
//        backgroundColor = Color("#dddddd")
//        padding = Padding(0.rem)
//        margin = Margin(0.rem)
//    }
//    "#container" {
//        marginLeft = LinearDimension.auto
//        marginRight = LinearDimension.auto
//        height = 100.vh
//        maxWidth = 400.px
//        backgroundColor = Color("#ffffff")
//        boxShadow.plusAssign(BoxShadow(Color.black.withAlpha(0.15), 0.px, 0.px, 15.px, 5.px))
//    }
//    "#header" {
//        width = 100.pct
//        backgroundColor = Color("#007cbc")
//
//    }
//    "#logo" {
//        margin = Margin(12.px, 18.px)
//        height = 40.px
//    }
//    "#form" {
//        width = 100.pct
//        height = LinearDimension("calc(100vh - 72px)")
//        borderWidth = 0.px
//    }
//}
//
//fun HTML.feedback() {
//    head {
//        title("Feedback | Parkeer Assistent")
//        meta {
//            name = "viewport"
//            content = "initial-scale=1"
//        }
//    }
//    body {
//        div {
//            id = "container"
//            div {
//                id = "header"
//                img {
//                    id = "logo"
//                    src = "/static/logo-transparent.svg"
//                }
//            }
//            div {
//                id = "main"
//                iframe {
//                    id = "form"
//                    src = "https://docs.google.com/forms/d/e/1FAIpQLSfhOv4kdt-8CORXakwPOm92zJQaFWOiPNV0LbzsDqD0EwN4Mg/viewform?embedded=true"
//                    +"Laden…"
//                }
//            }
//        }
//        style {
//            +Style.toString()
//        }
//    }
//}