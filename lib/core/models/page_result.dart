/// One page of a list (backend PageResponse). Pure Dart → usable in domain.
class PageResult<T> {
  final List<T> content;
  final int page; // 0-based
  final int size;
  final int totalElements;
  final int totalPages;

  const PageResult({
    required this.content,
    required this.page,
    required this.size,
    required this.totalElements,
    required this.totalPages,
  });

  bool get hasNext => page + 1 < totalPages;
}
