package benchmarks.complexlazylist.models

interface ICompositionItem {
    val bgColor: String?
    val radius: String?
    val alpha: String?
    val shadowColor: String?
    val textColor: String?
    val text: String?
}

interface ICompositionModel : IBaseViewModel {
    val title: String?
    val subtitle: String?
    val overlyTopLeft: ICompositionItem?
    val overlyView1: ICompositionItem?
    val overlyView2: ICompositionItem?
    val overlyView3: ICompositionItem?
    val overlyTopRight: ICompositionItem?
    val label: ICompositionItem?
}