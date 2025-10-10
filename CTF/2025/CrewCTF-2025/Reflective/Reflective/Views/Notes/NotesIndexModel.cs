namespace Reflective.Views.Notes;

public class NotesIndexModel
{
    public required int PageCount { get; init; }
    public required IEnumerable<Note> Notes { get; init; }
    public required int CurrentPage { get; init; }
    public required string SearchValue { get; init; }
}
