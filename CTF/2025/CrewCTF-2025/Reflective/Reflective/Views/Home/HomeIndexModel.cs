namespace Reflective.Views.Home;

public class HomeIndexModel
{
    public required int NoteCount { get; init; }
    public required IEnumerable<Note> Notes { get; init; }
}
