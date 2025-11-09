package nl.parkeerassistent.html
//
//import kotlinx.css.CssBuilder
//import kotlinx.css.Padding
//import kotlinx.css.body
//import kotlinx.css.padding
//import kotlinx.css.px
//import kotlinx.html.ButtonType
//import kotlinx.html.FormMethod
//import kotlinx.html.HTML
//import kotlinx.html.InputType
//import kotlinx.html.body
//import kotlinx.html.button
//import kotlinx.html.classes
//import kotlinx.html.form
//import kotlinx.html.head
//import kotlinx.html.input
//import kotlinx.html.link
//import kotlinx.html.meta
//import kotlinx.html.style
//import kotlinx.html.title
//
//private var Style = CssBuilder().apply {
//    body {
//        padding = Padding(50.px)
//    }
//}
//
//fun HTML.completeMockPayment() {
//    head {
//        title("Parkeer Assistent")
//        link {
//            rel = "stylesheet"
//            type = "text/css"
//            href = "https://cdn.jsdelivr.net/npm/bootstrap@4.5.3/dist/css/bootstrap.min.css"
//        }
//        meta {
//            name = "viewport"
//            content = "initial-scale=1"
//        }
//    }
//    body {
//        div {
//            form {
//                method = FormMethod.get
//                action = "https://parkeerassistent.nl/completePayment"
//                input {
//                    type = InputType.hidden
//                    name = "key1"
//                    value = "value1"
//                }
//                input {
//                    type = InputType.hidden
//                    name = "key2"
//                    value = "value3"
//                }
//                button {
//                    type = ButtonType.submit
//                    classes = setOf("btn", "btn-info")
//                    +"Terug naar app"
//                }
//            }
//        }
//        style {
//            +Style.toString()
//        }
//    }
//}
