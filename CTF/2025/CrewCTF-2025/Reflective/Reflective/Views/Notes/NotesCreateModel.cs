using System.ComponentModel.DataAnnotations;

namespace Reflective.Views.Notes;

public class NotesCreateModel
{
    [StringLength(100)]
    public required string Title { get; set; }

    public required string Description { get; set; }
}
