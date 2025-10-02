using Microsoft.AspNetCore.Mvc;
using Reflective.Views.Home;

namespace Reflective.Controllers;

[Route("/")]
[Controller]
public class HomeController : Controller
{
    private readonly INotesManager _manager;

    public HomeController(INotesManager manager)
    {
        this._manager = manager;
    }

    public IActionResult Index()
    {
        return View(new HomeIndexModel()
        {
            Notes = this._manager.GetLatestNotes(),
            NoteCount = this._manager.NoteCount
        });
    }
}
