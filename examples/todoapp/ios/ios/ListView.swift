import SwiftUI
import Todo

struct ListView: View {
    var items: [TodoItem]
    var onItemClicked: (_ id: Int64) -> Void
    var onDoneChanged: (_ id: Int64, _ isDone: Bool) -> Void
    
    var body: some View {
        List(self.items) { item in
            HStack {
                Text(item.text)
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
                    .background(Color.white)
                    .onTapGesture { withAnimation { self.onItemClicked(item.id) } }

                Image(systemName: item.isDone ? "checkmark.square" : "square")
                    .onTapGesture { self.onDoneChanged(item.id, !item.isDone) }
            }
        }.listStyle(PlainListStyle())
    }
}

struct ListView_Previews: PreviewProvider {
    static var previews: some View {
        ListView(
            items: [
                TodoItem(id: 1, order: 1, text: "Item 1", isDone: false),
                TodoItem(id: 2, order: 2, text: "Item 2", isDone: true),
                TodoItem(id: 3, order: 3, text: "Item 3", isDone: false)
            ],
            onItemClicked: {_ in },
            onDoneChanged: {_,_ in }
        )
    }
}

extension TodoItem : Identifiable {
}
