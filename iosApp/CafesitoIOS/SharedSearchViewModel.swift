import Foundation
import shared

@MainActor
final class SharedSearchViewModel: ObservableObject {
    @Published private(set) var state: SearchState

    private let wrapper: SearchViewModelWrapper
    private var closeable: Closeable?

    init() {
        let wrapper = SearchViewModelWrapper()
        self.wrapper = wrapper
        self.state = wrapper.state.value
        self.closeable = wrapper.state.watch { [weak self] newState in
            self?.state = newState
        }
    }

    deinit {
        closeable?.close()
    }

    func onQueryChange(_ query: String) {
        wrapper.onQueryChange(query: query)
    }

    func addSearchToHistory(_ term: String) {
        wrapper.addSearchToHistory(term: term)
    }

    func clearRecentSearches() {
        wrapper.clearRecentSearches()
    }
}
