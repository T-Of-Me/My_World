namespace Reflective;

public interface INotesManager
{
    int NoteCount { get; }
    IEnumerable<Note> GetLatestNotes(int page = 0);
    IEnumerable<Note> GetLatestNotes(string title, int page = 0);
    void AddNote(string title, string description);
}
