# Panel administrativo de CaptaBot – imagen para Docker Compose
# 1. Compilación con el SDK de .NET 10
FROM mcr.microsoft.com/dotnet/sdk:10.0 AS compilacion
WORKDIR /src
COPY CaptaBot.Panel/CaptaBot.Panel.csproj CaptaBot.Panel/
RUN dotnet restore CaptaBot.Panel/CaptaBot.Panel.csproj
COPY CaptaBot.Panel/ CaptaBot.Panel/
RUN dotnet publish CaptaBot.Panel/CaptaBot.Panel.csproj -c Release -o /app --no-restore

# 2. Ejecución con el runtime de ASP.NET Core
FROM mcr.microsoft.com/dotnet/aspnet:10.0
WORKDIR /app
COPY --from=compilacion /app .
ENV ASPNETCORE_HTTP_PORTS=8080
EXPOSE 8080
USER $APP_UID
ENTRYPOINT ["dotnet", "CaptaBot.Panel.dll"]
