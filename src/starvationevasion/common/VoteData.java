package starvationevasion.common;


import com.oracle.javafx.jmx.json.JSONDocument;

import starvationevasion.common.gamecards.GameCard;
import starvationevasion.server.model.Sendable;
import starvationevasion.server.model.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VoteData implements Sendable
{
  private ArrayList<GameCard> ballot = new ArrayList<>();
  private ArrayList<GameCard> enacted = new ArrayList<>();
  private GameCard[][] cards = new GameCard[EnumRegion.US_REGIONS.length][2];


  public VoteData(ArrayList<GameCard> ballot, ArrayList<GameCard> enacted, GameCard[][] cards)
  {
    this.ballot = ballot;
    this.enacted = enacted;
    this.cards = cards;
  }

  public ArrayList<GameCard> getBallot ()
  {
    return ballot;
  }

  public ArrayList<GameCard> getEnacted ()
  {
    return enacted;
  }

  public int getVotes(EnumRegion region)
  {
    for (GameCard card : cards[region.ordinal()])
    {
      if (card.votesRequired() > 0)
      {
        return card.votesRequired();
      }
    }
    return -1;
  }

  public List<GameCard> getRegionCards (EnumRegion region)
  {
    return Arrays.asList(cards[region.ordinal()]);
  }

  @Override
  public Type getType ()
  {
    return Type.VOTE_RESULTS;
  }

  @Override
  public JSONDocument toJSON ()
  {
    JSONDocument _json = JSONDocument.createObject();
    JSONDocument _jsonEnacting = JSONDocument.createArray(enacted.size());
    JSONDocument _jsonBallot = JSONDocument.createArray(ballot.size());




    for (int i = 0; i < enacted.size(); i++)
    {
      _jsonEnacting.set(i, enacted.get(i).toJSON());
    }

    for (int i = 0; i < ballot.size(); i++)
    {
      // get the card's JSON
      JSONDocument _doc = ballot.get(i).toJSON();
      // add a the votes to it
      _doc.setNumber("votes-received", ballot.get(i).getEnactingRegionCount());
      // create an array
      JSONDocument _jsonRe = JSONDocument.createArray(EnumRegion.US_REGIONS.length);

      // iter through the regions and check if their vote
      for (int j = 0; j < EnumRegion.US_REGIONS.length; j++)
      {
        if (ballot.get(i).didVoteYes(EnumRegion.values()[j]))
        {
          // if they did add them to the array
          _jsonRe.set(i, EnumRegion.values()[j].toJSON());
        }
      }
      // add the array of enacting regions to card
      _doc.set("enacting-regions", _jsonRe);
      // add to ballot array
      _jsonBallot.set(i, _doc);
    }


    _json.set("ballot", _jsonBallot);
    _json.set("enacted", _jsonEnacting);

    return _json;
  }

  @Override
  public void fromJSON (Object doc)
  {

  }
}
