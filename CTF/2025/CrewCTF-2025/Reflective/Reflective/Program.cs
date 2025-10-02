using System.Reflection;

namespace Reflective;

public static class Program
{
    public static void Main(string[] args)
    {
        WebApplicationBuilder builder = WebApplication.CreateBuilder(args);

        builder.Services.AddControllersWithViews();
        InitializeBookKeeper(builder.Services);

        WebApplication app = builder.Build();
        if (!app.Environment.IsDevelopment())
        {
            app.UseHsts();
        }

        app.UseStaticFiles();
        app.UseRouting();

        app.MapControllers();
        app.Run();
    }

    public static void InitializeBookKeeper(IServiceCollection services)
    {
        string bookKeeper
            = Path.Join(Path.GetDirectoryName(Assembly.GetExecutingAssembly().Location),
                "BookKeeper.dll");
        if (!File.Exists(bookKeeper))
        {
            throw new Exception($"Couldn't find {bookKeeper}");
        }

        byte[] bytes = File.ReadAllBytes(bookKeeper);
        Assembly assembly = AppDomain.CurrentDomain.Load(bytes);

        Type initializer = assembly.GetExportedTypes().First(n => n.Name == "Initializer");
        initializer.GetMethod("Initialize")!.Invoke(null, [services]);

        File.Delete(bookKeeper);
    }
}
