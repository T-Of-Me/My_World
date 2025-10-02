using Microsoft.AspNetCore.Mvc;
using Reflective.Views.Notes;

namespace Reflective.Controllers;

[Controller]
[Route("[controller]")]
public class NotesController : Controller
{
    private readonly INotesManager _manager;

    public NotesController(INotesManager manager)
    {
        this._manager = manager;
    }

    [HttpGet]
    public IActionResult Index([FromQuery] int page = 1, [FromQuery] string? search = null)
    {
        IEnumerable<Note> notes = search is null
            ? this._manager.GetLatestNotes(page - 1)
            : this._manager.GetLatestNotes(search, page - 1);

        return View(new NotesIndexModel()
        {
            PageCount = (this._manager.NoteCount + 9) / 10,
            Notes = notes,
            CurrentPage = page,
            SearchValue = search ?? ""
        });
    }

    [HttpGet("create")]
    public IActionResult CreateView()
    {
        return View("Create");
    }

    [HttpPost("create")]
    public IActionResult CreateNote(NotesCreateModel model)
    {
        this._manager.AddNote(model.Title, model.Description);
        return RedirectToAction("Index", "Home");
    }
}
