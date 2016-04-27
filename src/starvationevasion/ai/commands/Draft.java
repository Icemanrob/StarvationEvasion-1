package starvationevasion.ai.commands;

import java.util.ArrayList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;

import starvationevasion.ai.AI;
import starvationevasion.ai.AI.WorldFactors;
import starvationevasion.common.EnumFood;
import starvationevasion.common.EnumFood;
import starvationevasion.common.EnumRegion;
import starvationevasion.common.EnumRegion;
import starvationevasion.common.Util;
import starvationevasion.common.Util;
import starvationevasion.common.policies.EnumPolicy;
import starvationevasion.common.policies.PolicyCard;
import starvationevasion.common.policies.Policy_EfficientIrrigationIncentive;
import starvationevasion.common.policies.Policy_EthanolTaxCreditChange;
import starvationevasion.common.policies.Policy_FertilizerSubsidy;
import starvationevasion.server.model.Endpoint;
import starvationevasion.server.model.Payload;
import starvationevasion.server.model.RequestFactory;
import starvationevasion.server.model.State;



public class Draft extends AbstractCommand
{
  private boolean draftedCard = false;
  private boolean discarded = false;
  private boolean drawn = false;
  private PolicyCard cardDrafted;
  private PolicyCard[] draftedCards = new PolicyCard[2];
  private int tries = 2;
  private int numTurns = 0;
  // The region that the AI represents.
  String region = "";
  Long foodProduced = new Long(0);
  Integer foodIncome = new Integer(0);
  Long foodImported = new Long(0);
  Long foodExported = new Long(0);
  Long lastFoodProduced = new Long(0);
  Integer lastFoodIncome = new Integer(0);
  Long lastFoodImported = new Long(0);
  Long lastFoodExported = new Long(0);
  int probModifier = 0;
  int z = 0;
  int draftIndex = 0;

  public Draft(AI client,String region)
  {
    super(client);
    this.numTurns = client.numTurns;
    this.region = region;
  }

  @Override
  public String commandString()
  {
    return "Draft";
  }

  @Override
  public boolean run ()
  {
//    System.out.println("Drafted: " + draftedCard +
//                               "\nDiscarded: " + discarded +
//                               "\nDrawn: " + drawn);

    if (!getClient().getState().equals(State.DRAFTING) && tries > 0)
    {
      tries--;
      if (tries <= 0)
      {
        return false;
      }
    }



    if (getClient().getState().equals(State.DRAFTING))
    {

      if (!draftedCard)
      {
        randomlySetDraftedCard();
        return true;
      }

      if (!discarded && getClient().getUser().getHand().size() > 0)
      {
        if (!Util.rand.nextBoolean())
        {
          randomlyDiscard();
        }

        return true;
      }
      getClient().send(new RequestFactory().build(getClient().getStartNanoSec(), new Payload(), Endpoint.DONE));

//      if (!drawn && getClient().getUser().getHand().size() < 7)
//      {
//        Request request = new Request(getClient().getStartNanoSec(), Endpoint.DRAW_CARD);
//        getClient().send(request);
//        drawn = true;
//        return true;
//      }

    }
    return false;
  }

  private void randomlyDiscard()
  {
    EnumPolicy discard = null;

    for (EnumPolicy policy : getClient().getUser().getHand())
    {
      PolicyCard card = PolicyCard.create(getClient().getUser().getRegion(),
          policy);
      // dont remove the card we just drafted!!!
      if (cardDrafted != null && policy != cardDrafted.getCardType()
          && Util.rand.nextBoolean())
      {
        discard = policy;
        break;
      }
    }
    int idx = getClient().getUser().getHand().indexOf(discard);
    if (idx >= 0)
    {

      getClient().send(new RequestFactory().build(getClient().getStartNanoSec(),
          discard, Endpoint.DELETE_CARD));

    }
    discarded = true;
  }

  /**
   * Jeffrey McCall If this is the first turn, then randomly choose the card to
   * draft. If this is not the first turn, then evaluate choice of next card to
   * draft according to how things turned out after the last turn. If after the
   * last turn, there was an overall decrease in factors that are relevant to
   * this AI's region, then it is less likely that the AI will again choose that
   * food, region or policy.
   */
  private void randomlySetDraftedCard()
  {
    PolicyCard card = null;

    int i = 0;
    boolean draftSent = false;
    Random rand = new Random();
    LinkedList<String> policySampleSpace = new LinkedList<String>();
    LinkedList<String> foodSampleSpace = new LinkedList<String>();
    LinkedList<String> regionSampleSpace = new LinkedList<String>();
    for (i = 0; i < EnumPolicy.values().length; i++)
    {
      for (int h = 0; h < 3; h++)
      {
        policySampleSpace.add(EnumPolicy.values()[i].name());
      }
    }
    for (i = 0; i < EnumFood.values().length; i++)
    {
      for (int h = 0; h < 3; h++)
      {
        foodSampleSpace.add(EnumFood.values()[i].name());
      }
    }
    for (i = 0; i < EnumRegion.values().length; i++)
    {
      for (int h = 0; h < 3; h++)
      {
        regionSampleSpace.add(EnumRegion.values()[i].name());
      }
    }
    i = 0;
    if (numTurns == 0)
    {
      int firstRandNum = rand.nextInt(7);
      int secondRandNum = 0;
      do
      {
        secondRandNum = rand.nextInt(7);
      } while (secondRandNum == firstRandNum);
      for (EnumPolicy policy : getClient().getUser().getHand())
      {
        if (i == firstRandNum || i == secondRandNum)
        {
          // TODO will have to change this in the future. Cards will be changed
          // to not have BOTH a
          // target region and target food to set, it will be one of each.
          card = PolicyCard.create(getClient().getUser().getRegion(), policy);
          EnumFood[] foods = card.getValidTargetFoods();
          EnumRegion[] regions = card.getValidTargetRegions();
          EnumFood food=null;
          if(foods!=null)
          {
            food = foods[rand.nextInt(foods.length)];
          }
          EnumRegion region=null;
          if(regions!=null)
          {
            region = regions[rand.nextInt(regions.length)];
          }
          setupCard(card, food, region);
          getClient().send(new RequestFactory()
              .build(getClient().getStartNanoSec(), card, Endpoint.DRAFT_CARD));
          draftedCard = true;
          draftSent = true;
          cardDrafted = card;
          getClient().draftedCards.get(numTurns).add(card);
        }
        i++;
      }
      tries = 2;
    } else
    {
      System.out.println("Last hand size:"+getClient().draftedCards.get(numTurns-1).size());
      PolicyCard lastCard1 = getClient().draftedCards.get(numTurns - 1).get(0);
      PolicyCard lastCard2 = getClient().draftedCards.get(numTurns - 1).get(1);
      int playAgain = 0;
      playAgain = checkLastPlay();
      foodProduced = (long) 0;
      foodIncome = 0;
      foodImported = (long) 0;
      foodExported = (long) 0;
      lastFoodProduced = (long) 0;
      lastFoodIncome = 0;
      lastFoodImported = (long) 0;
      lastFoodExported = (long) 0;
      if (lastCard1.getTargetFood() != null)
      {
        distributeProbabilities(playAgain, foodSampleSpace, lastCard1, "food");
      }
      if (lastCard1.getTargetRegion() != null)
      {
        distributeProbabilities(playAgain, regionSampleSpace, lastCard1,
            "region");
      }
      distributeProbabilities(playAgain, policySampleSpace, lastCard1,
          "policy");
      if (lastCard2.getTargetFood() != null)
      {
        distributeProbabilities(playAgain, foodSampleSpace, lastCard2, "food");
      }
      if (lastCard2.getTargetRegion() != null)
      {
        distributeProbabilities(playAgain, regionSampleSpace, lastCard2,
            "region");
      }
      distributeProbabilities(playAgain, policySampleSpace, lastCard2,
          "policy");
      draftCards(rand, policySampleSpace, foodSampleSpace, regionSampleSpace);
      draftCards(rand, policySampleSpace, foodSampleSpace, regionSampleSpace);
      draftedCard = true;
      draftSent=true;
      tries = 2;
    }
    // for (EnumPolicy policy : getClient().getUser().getHand())
    // {
    // card = PolicyCard.create(getClient().getUser().getRegion(), policy);
    //
    // if (card.votesRequired() == 0 || !draftSent)
    // {
    // setupCard(card);
    // getClient().send(new RequestFactory()
    // .build(getClient().getStartNanoSec(), card, Endpoint.DRAFT_CARD));
    // draftedCard = true;
    // draftSent = true;
    // if (i == 0)
    // {
    // continue;
    // }
    // if (i == getClient().getUser().getHand().size() - 1)
    // {
    // return;
    // }
    //
    // }
    // i++;
    // }
    // setupCard(card);
    //
    // if (card == null)
    // {
    // return;
    // }
    //
    // new RequestFactory().build(getClient().getStartNanoSec(), card,
    // Endpoint.DRAFT_CARD);
    // cardDrafted = card;
    // draftedCard = true;
    // tries = 2;
  }

  public void draftCards(Random rand, LinkedList<String> policySampleSpace,
      LinkedList<String> foodSampleSpace, LinkedList<String> regionSampleSpace)
  {
    PolicyCard card = null;
    ArrayList<String> currentHand = new ArrayList<String>();
    Map<String, EnumPolicy> policyMap = new HashMap<>();
    for (EnumPolicy policy : getClient().getUser().getHand())
    {
      currentHand.add(policy.name());
      policyMap.put(policy.name(), policy);
    }
    String policy = "";
    do
    {
      int policyIndex = rand.nextInt(policySampleSpace.size());
      policy = policySampleSpace.get(policyIndex);
    } while (!currentHand.contains(policy));
    EnumPolicy currentPolicy = policyMap.get(policy);
    card = PolicyCard.create(getClient().getUser().getRegion(), currentPolicy);
    EnumFood[] foods = card.getValidTargetFoods();
    ArrayList<String> validFoods = new ArrayList<>();
    Map<String, EnumFood> foodMap = new HashMap<>();
    EnumFood currentFood = null;
    if (foods != null)
    {
      Stream.of(foods).forEach(food ->
      {
        validFoods.add(food.name());
        foodMap.put(food.name(), foods[z]);
        z++;
      });
      z = 0;
      String food = "";
      do
      {
        int foodIndex = rand.nextInt(foodSampleSpace.size());
        food = foodSampleSpace.get(foodIndex);
      } while (!validFoods.contains(food));
      currentFood = foodMap.get(food);
    }
    EnumRegion[] regions = card.getValidTargetRegions();
    ArrayList<String> validRegions = new ArrayList<String>();
    Map<String, EnumRegion> regionMap = new HashMap<>();
    EnumRegion currentRegion = null;
    if (regions != null)
    {
      Stream.of(regions).forEach(region ->
      {
        validRegions.add(region.name());
        regionMap.put(region.name(), regions[z]);
        z++;
      });
      z = 0;
      String region = "";
      do
      {
        int regionIndex = rand.nextInt(regionSampleSpace.size());
        region = regionSampleSpace.get(regionIndex);
      } while (!validRegions.contains(region));
      currentRegion = regionMap.get(region);
    }
    setupCard(card, currentFood, currentRegion);
    getClient().send(new RequestFactory().build(getClient().getStartNanoSec(),
        card, Endpoint.DRAFT_CARD));
    cardDrafted = card;
    draftedCards[draftIndex] = card;
    getClient().draftedCards.get(numTurns).add(card);
    draftIndex++;
  }

  /**
   * Jeffrey McCall This method will change the probability values in an array
   * of probability values. The probability is decreased if playAgain==-1. It is
   * increased if playAgain==1.
   * 
   * @param playAgain
   *          An int value which determines how the probability is altered.
   * @param sampleSpace
   *          The linkedlist of Strings that represents what values will be
   *          chosen for the next card that is played.
   * @param card
   *          The policy card that was played last turn that we are checking.
   * @param type
   *          The type of item that the probabilities relate to.
   * @return The index of the value that was altered in the array as well as the
   *         new value that it was changed to.
   */
  public void distributeProbabilities(int playAgain,
      LinkedList<String> sampleSpace, PolicyCard card, String type)
  {
    if (playAgain == -1)
    {
      for (int i = 0; i < sampleSpace.size(); i++)
      {
        if (type.equals("food"))
        {
          if (card.getTargetFood().name().equals(sampleSpace.get(i)))
          {
            sampleSpace.remove(i);
            return;
          }
        } else if (type.equals("region"))
        {
          if (card.getTargetRegion().name().equals(sampleSpace.get(i)))
          {
            sampleSpace.remove(i);
            return;
          }
        } else if (type.equals("policy"))
        {
          if (card.getPolicyName().equals(sampleSpace.get(i)))
          {
            sampleSpace.remove(i);
            return;
          }
        }
      }
    } else if (playAgain == 1)
    {
      for (int i = 0; i < sampleSpace.size(); i++)
      {
        if (type.equals("food"))
        {
          if (card.getTargetFood().name().equals(sampleSpace.get(i)))
          {
            for (int h = 0; h < 12; h++)
            {
              sampleSpace.add(i, card.getTargetFood().name());
            }
            return;
          }
        } else if (type.equals("region"))
        {
          if (card.getTargetRegion().name().equals(sampleSpace.get(i)))
          {
            for (int h = 0; h < 12; h++)
            {
              sampleSpace.add(i, card.getTargetRegion().name());
            }
            return;
          }
        } else if (type.equals("policy"))
        {
          if (card.getPolicyName().equals(sampleSpace.get(i)))
          {
            for (int h = 0; h < 12; h++)
            {
              sampleSpace.add(i, card.getPolicyName());
            }
            return;
          }
        }
      }
    }
  }

  /**
   * Jeffrey McCall Go through the relevant regional factors to determine how
   * things have worsened or improved since the last turn. If the amount of
   * decrease of factors is greater than the increase, then this method will
   * return -1 and the card in question will be less likely to get played this
   * turn. If the amount of decrease is equal to the increase, than 0 is
   * returned and the likelihood of the card getting played this turn is
   * unaffected. If there was an overall improvement of factors greater than the
   * decrease, then it is more likely that the policy card will get drafted
   * again.
   * 
   * @return -1, 0 or 1
   */
  private int checkLastPlay()
  {
    int overallPercentDecrease = 0;
    int overallPercentIncrease = 0;
    Integer revenueBalance=new Integer(0);
    Double undernourished=new Double(0);
    Double hdi=new Double(0);
    Integer lastRevenueBalance=new Integer(0);
    Double lastUndernourished=new Double(0);
    Double lastHdi=new Double(0);
    System.out.println("World data size:"+getClient().worldData.size());
    for(int h=0;h<getClient().worldData.size();h++)
    {
      if(h==0)
      {
        lastRevenueBalance = (Integer) getClient().factorMap
            .get(WorldFactors.REVENUEBALANCE).get(h)[0];
        lastUndernourished = (Double) getClient().factorMap
            .get(WorldFactors.UNDERNOURISHED).get(h)[0];
        lastHdi = (Double) getClient().factorMap.get(WorldFactors.HDI)
            .get(h)[0];
        Stream.of(getClient().factorMap.get(WorldFactors.FOODPRODUCED).get(h))
          .forEach(val ->
          {
            lastFoodProduced += (Long) val;
          });
        Stream.of(getClient().factorMap.get(WorldFactors.FOODINCOME).get(h))
          .forEach(val ->
          {
            lastFoodIncome += (Integer) val;
          });
        Stream.of(getClient().factorMap.get(WorldFactors.FOODIMPORTED).get(h))
          .forEach(val ->
          {
            lastFoodImported += (Long) val;
          });
        Stream.of(getClient().factorMap.get(WorldFactors.FOODEXPORTED).get(h))
          .forEach(val ->
          {
            lastFoodExported += (Long) val;
          });
      }
      if(h==1)
      {
        revenueBalance = (Integer) getClient().factorMap
            .get(WorldFactors.REVENUEBALANCE).get(h)[0];
        undernourished = (Double) getClient().factorMap.get(WorldFactors.UNDERNOURISHED)
            .get(h)[0];
        hdi = (Double) getClient().factorMap.get(WorldFactors.HDI).get(h)[0];
        Stream.of(getClient().factorMap.get(WorldFactors.FOODPRODUCED).get(h))
          .forEach(val ->
          {
            foodProduced += (Long) val;
          });
        Stream.of(getClient().factorMap.get(WorldFactors.FOODINCOME).get(h))
          .forEach(val ->
          {
            foodIncome += (Integer) val;
          });
        Stream.of(getClient().factorMap.get(WorldFactors.FOODIMPORTED).get(h))
          .forEach(val ->
          {
            foodImported += (Long) val;
          });
        Stream.of(getClient().factorMap.get(WorldFactors.FOODEXPORTED).get(h))
          .forEach(val ->
          {
            foodExported += (Long) val;
          });
        Object[][] numArray =
          {
              { lastRevenueBalance, revenueBalance },
              { lastUndernourished, undernourished },
              { lastHdi, hdi },
              { lastFoodProduced, foodProduced },
              { lastFoodIncome, foodIncome },
              { lastFoodImported, foodImported },
              { lastFoodExported, foodExported } };
          for (int i = 0; i < 7; i++)
          {
            if (i == 0 || i == 4)
            {
              int percentChange = percentChangeInt((int) numArray[i][0],
                  (int) numArray[i][1]);
              if (percentChange < 0)
              {
                overallPercentDecrease += (percentChange * -1);
              } else if (percentChange > 0)
              {
                overallPercentIncrease += percentChange;
              }
            } else if (i == 1)
            {
              double percentChange = percentChangeDouble((double) numArray[i][0],
                  (double) numArray[i][1]);
              if (percentChange > 0)
              {
                overallPercentDecrease += percentChange;
              } else if (percentChange < 0)
              {
                overallPercentIncrease += (percentChange * -1);
              }
            } else if (i == 2)
            {
              double percentChange = percentChangeDouble((double) numArray[i][0],
                  (double) numArray[i][1]);
              if (percentChange < 0)
              {
                overallPercentDecrease += (percentChange * -1);
              } else if (percentChange > 0)
              {
                overallPercentIncrease += percentChange;
              }
            } else if (i == 3 || i > 4)
            {
              long percentChange = percentChangeLong((long) numArray[i][0],
                  (long) numArray[i][1]);
              if (percentChange < 0)
              {
                overallPercentDecrease += (percentChange * -1);
              } else if (percentChange > 0)
              {
                overallPercentIncrease += percentChange;
              }
            }
          }
          if (overallPercentIncrease > overallPercentDecrease)
          {
            return 1;
          } else if (overallPercentDecrease > overallPercentIncrease)
          {
            return -1;
          }
      }
    }
    return 0;
  }

  private int percentChangeInt(int originalVal, int newVal)
  {
    if (originalVal != 0)
    {
      return ((originalVal - newVal) / originalVal) * 100;
    } else
    {
      return 0;
    }
  }

  private double percentChangeDouble(double originalVal, double newVal)
  {
    if (originalVal != 0)
    {
      return ((originalVal - newVal) / originalVal) * 100;
    } else
    {
      return 0;
    }
  }

  private long percentChangeLong(long originalVal, long newVal)
  {
    if (originalVal != 0)
    {
      return ((originalVal - newVal) / originalVal) * 100;
    } else
    {
      return 0;
    }
  }

  private void setupCard(PolicyCard card,EnumFood food,EnumRegion region)
  {
    if (card == null)
    {
      return;
    }

    if(food!=null)
    {
      card.setTargetFood(food);
    }
    if(region!=null)
    {
      card.setTargetRegion(region);
    }
    for (PolicyCard.EnumVariable variable : PolicyCard.EnumVariable.values())
    {
      if (card.getRequiredVariables(variable) != null)
      {
        int amt = 0;
        // Contextually setting variable
        if (card.getRequiredVariables(variable).equals(PolicyCard.EnumVariableUnit.MILLION_DOLLAR))
        {
          amt = Util.randInt(1, 20);
        }
        else if (card.getRequiredVariables(variable).equals(PolicyCard.EnumVariableUnit.PERCENT))
        {
          if (card instanceof EfficientIrrigationIncentivePolicy)
          {
            amt = Util.randInt(50, 80);
          }
          else if (card instanceof EthanolTaxCreditChangePolicy || card instanceof FertilizerSubsidyPolicy)
          {
            amt = Util.randInt(20, 40);
          }
          else
          {
            amt = Util.randInt(1, 100);
          }
        }
        else if (card.getRequiredVariables(variable).equals(PolicyCard.EnumVariableUnit.UNIT))
        {
          amt = Util.randInt(1, 10);
        }

        if (variable.name().equals("X"))
        {
          card.setX(amt);
          if (card instanceof CovertIntelligencePolicy)
          {
            if (Util.rand.nextBoolean())
            {
              card.setX(2);
            }
            else
            {
              card.setX(7);
            }
            break;
          }
        }
        else if (variable.name().equals("Y"))
        {
          card.setY(amt);
        }
        else if (variable.name().equals("Z"))
        {
          card.setZ(amt);
        }
      }

    }
  }
}
