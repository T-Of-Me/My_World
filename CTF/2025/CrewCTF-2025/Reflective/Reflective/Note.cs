namespace Reflective;

public class Note
{
    public required string Title { get; init; }
    public required string Description { get; init; }
    public DateTime CreatedAt { get; init; } = DateTime.UtcNow;
}
