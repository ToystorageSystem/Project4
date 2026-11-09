/// Move stock between the backroom and the shelf (backend MoveDirection)
enum MoveDirection {
  toShelf('TO_SHELF', 'Move to shelf'),
  toBackroom('TO_BACKROOM', 'Move to backroom');

  const MoveDirection(this.apiValue, this.label);
  final String apiValue;
  final String label;
}
