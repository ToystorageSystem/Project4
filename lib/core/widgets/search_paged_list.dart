import 'package:flutter/material.dart';

import '../constants/app_colors.dart';

/// Lower-case, strip Vietnamese diacritics (đ → d) so "kệ a" matches "Ke A".
String foldSearch(String? value) {
  const from = 'àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ';
  const to = 'aaaaaaaaaaaaaaaaaeeeeeeeeeeeiiiiiooooooooooooooooouuuuuuuuuuuyyyyyd';
  final lower = (value ?? '').toLowerCase();
  final buffer = StringBuffer();
  for (final rune in lower.runes) {
    final ch = String.fromCharCode(rune);
    final i = from.indexOf(ch);
    buffer.write(i >= 0 ? to[i] : ch);
  }
  return buffer.toString();
}

/// Every word of [query] must appear somewhere in [values].
bool matchesSearch(String query, Iterable<String?> values) {
  final q = foldSearch(query).trim();
  if (q.isEmpty) return true;
  final haystack = values.map(foldSearch).join(' ');
  return q.split(RegExp(r'\s+')).every(haystack.contains);
}

/// Search box with a clear button, used above lists.
class ListSearchField extends StatelessWidget {
  const ListSearchField({
    super.key,
    required this.controller,
    required this.onChanged,
    this.hint = 'Search…',
  });

  final TextEditingController controller;
  final ValueChanged<String> onChanged;
  final String hint;

  @override
  Widget build(BuildContext context) {
    return TextField(
      controller: controller,
      onChanged: onChanged,
      textInputAction: TextInputAction.search,
      decoration: InputDecoration(
        hintText: hint,
        isDense: true,
        filled: true,
        fillColor: Colors.white,
        prefixIcon: const Icon(Icons.search_rounded),
        suffixIcon: controller.text.isEmpty
            ? null
            : IconButton(
                tooltip: 'Clear',
                icon: const Icon(Icons.close_rounded),
                onPressed: () {
                  controller.clear();
                  onChanged('');
                },
              ),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide.none,
        ),
      ),
    );
  }
}

/// "‹ Page 2/5 ›" bar with a "21–40 of 93" caption.
class ListPagerBar extends StatelessWidget {
  const ListPagerBar({
    super.key,
    required this.page,
    required this.pages,
    required this.total,
    required this.pageSize,
    required this.onPage,
    this.label = 'items',
  });

  final int page; // 0-based
  final int pages;
  final int total;
  final int pageSize;
  final ValueChanged<int> onPage;
  final String label;

  @override
  Widget build(BuildContext context) {
    if (total == 0) return const SizedBox.shrink();
    final from = page * pageSize + 1;
    final to = (from + pageSize - 1).clamp(0, total);
    return Padding(
      padding: const EdgeInsets.only(top: 4),
      child: Row(
        children: [
          Expanded(
            child: Text(
              '$from–$to of $total $label',
              style: const TextStyle(color: AppColors.textGrey, fontSize: 12),
            ),
          ),
          if (pages > 1) ...[
            IconButton(
              tooltip: 'Previous page',
              visualDensity: VisualDensity.compact,
              onPressed: page > 0 ? () => onPage(page - 1) : null,
              icon: const Icon(Icons.chevron_left_rounded),
            ),
            Text('${page + 1}/$pages'),
            IconButton(
              tooltip: 'Next page',
              visualDensity: VisualDensity.compact,
              onPressed: page < pages - 1 ? () => onPage(page + 1) : null,
              icon: const Icon(Icons.chevron_right_rounded),
            ),
          ],
        ],
      ),
    );
  }
}

/// A scrollable list with a search box and client-side pages.
/// [header] widgets are shown above the search box, [emptyText] when nothing matches.
class SearchPagedList<T> extends StatefulWidget {
  const SearchPagedList({
    super.key,
    required this.items,
    required this.searchValues,
    required this.itemBuilder,
    this.header = const [],
    this.hint = 'Search…',
    this.emptyText = 'Nothing matches your search.',
    this.label = 'items',
    this.pageSize = 20,
    this.spacing = 10,
    this.padding = const EdgeInsets.fromLTRB(16, 12, 16, 24),
  });

  final List<T> items;
  final Iterable<String?> Function(T item) searchValues;
  final Widget Function(BuildContext context, T item) itemBuilder;
  final List<Widget> header;
  final String hint;
  final String emptyText;
  final String label;
  final int pageSize;
  final double spacing;
  final EdgeInsets padding;

  @override
  State<SearchPagedList<T>> createState() => _SearchPagedListState<T>();
}

class _SearchPagedListState<T> extends State<SearchPagedList<T>> {
  final _controller = TextEditingController();
  String _query = '';
  int _page = 0;

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final matches = widget.items
        .where((item) => matchesSearch(_query, widget.searchValues(item)))
        .toList();
    final pages = matches.isEmpty
        ? 1
        : (matches.length + widget.pageSize - 1) ~/ widget.pageSize;
    final page = _page.clamp(0, pages - 1);
    final start = page * widget.pageSize;
    final visible = matches.skip(start).take(widget.pageSize).toList();

    return ListView(
      physics: const AlwaysScrollableScrollPhysics(),
      padding: widget.padding,
      children: [
        ...widget.header,
        ListSearchField(
          controller: _controller,
          hint: widget.hint,
          onChanged: (value) => setState(() {
            _query = value;
            _page = 0;
          }),
        ),
        SizedBox(height: widget.spacing),
        if (visible.isEmpty)
          Padding(
            padding: const EdgeInsets.symmetric(vertical: 32),
            child: Center(
              child: Text(
                widget.emptyText,
                style: const TextStyle(color: AppColors.textGrey),
              ),
            ),
          ),
        for (final item in visible) ...[
          widget.itemBuilder(context, item),
          SizedBox(height: widget.spacing),
        ],
        ListPagerBar(
          page: page,
          pages: pages,
          total: matches.length,
          pageSize: widget.pageSize,
          label: widget.label,
          onPage: (p) => setState(() => _page = p),
        ),
      ],
    );
  }
}
