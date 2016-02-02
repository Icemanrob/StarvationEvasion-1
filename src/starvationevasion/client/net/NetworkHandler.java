package starvationevasion.client.net;


import starvationevasion.client.controller.AbstractPlayerController;
import starvationevasion.client.controller.ComputerPlayerController;
import starvationevasion.client.controller.HumanPlayerController;
import starvationevasion.client.model.GameOptions;
import starvationevasion.client.net.common.NetworkStatus;
import starvationevasion.client.model.Player;
import starvationevasion.client.model.GameStateData;

/**
 * Creates Clients and if a client is lost it tells the Game engine.
 */
public class NetworkHandler
{
  private final GameOptions options;
  private AbstractClient playerClient;
  private GameStateData gameState;
  private Player player;


  public NetworkHandler(GameOptions options, GameStateData gameState)
  {
    this.options = options;
    this.gameState = gameState;
  }

  /**
   * Connects the main client to a desired server.
   * This needs to happen first because we need to know the status
   * of the server, specifically how many people are on it! if there are
   * already filled then we need to figure out how to handle AI.
   */
  public void createClient(AbstractPlayerController playerController)
  {
    if (playerController instanceof HumanPlayerController)
    {
      playerClient = new HumanClient(options, playerController.getPlayer(), gameState);
    }
    else if (playerController instanceof ComputerPlayerController)
    {
      playerClient = new ComputerClient(options, playerController.getPlayer(), gameState);
    }
    player = playerController.getPlayer();
    playerClient.start();
    playerController.setClient(playerClient);
  }

  public AbstractClient getOurClient()
  {
    return playerClient;
  }

  /**
   * Connects the client to the server.
   */
  public void connectToServer()
  {
    if (options.getNetworkStatus() != NetworkStatus.CONNECTED)
    {
      playerClient.connect();
    }
  }

  /**
   * Logs the player into the server.
   */
  public void loginToServer()
  {
    playerClient.login();
  }

  /**
   * Disconnects a player from the server.
   */
  public void disconnectFromServer()
  {
    if (getOurClient() != null && getOurClient().isConnected())
    {
      playerClient.disconnect();
    }
    else
    {
      options.setNetworkStatus(NetworkStatus.NOT_CONNECTED);
    }
  }
}