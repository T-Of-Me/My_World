using Microsoft.Extensions.DependencyInjection;
using Reflective;

namespace BookKeeper;

public class Initializer
{
    public static void Initialize(IServiceCollection services)
    {
        services.AddSingleton<INotesManager, NotesManager>();
    }
}
