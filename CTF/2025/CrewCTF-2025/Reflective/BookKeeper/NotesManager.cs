using System.Linq.Dynamic.Core;
using Reflective;

namespace BookKeeper;

public class NotesManager : INotesManager
{
    private readonly List<Note> _notes =
    [
        new()
        {
            Title = "Donate to Ayame",
            Description
                = "Remember to donate 75% of your income to Nakiri Ayame. Your kamioshi is more important than yourself and deserves all your money!"
        }
    ];

    private static readonly string _flag = "crew{fake_flag}";

    public int NoteCount => this._notes.Count;

    public IEnumerable<Note> GetLatestNotes(int page = 0)
    {
        return this._notes.OrderByDescending(n => n.CreatedAt).Skip(page * 10).Take(10);
    }

    public IEnumerable<Note> GetLatestNotes(string title, int page = 0)
    {
        string query = "Title.Contains(\"" + title + "\")";

        return this._notes
            .AsQueryable()
            .OrderByDescending(n => n.CreatedAt)
            .Where(query)
            .Skip(page * 10)
            .Take(10);
    }

    public void AddNote(string title, string description)
    {
        this._notes.Add(new Note()
        {
            Title = title,
            Description = description
        });
    }
}
