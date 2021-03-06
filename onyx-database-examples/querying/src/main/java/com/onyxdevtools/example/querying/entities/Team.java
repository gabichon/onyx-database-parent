package com.onyxdevtools.example.querying.entities;

import com.onyx.persistence.IManagedEntity;
import com.onyx.persistence.ManagedEntity;
import com.onyx.persistence.annotations.*;
import com.onyx.persistence.annotations.values.CascadePolicy;
import com.onyx.persistence.annotations.values.FetchPolicy;
import com.onyx.persistence.annotations.values.RelationshipType;

import java.util.List;

/**
 * @author Chris Osborn
 */
//J-
@Entity
@SuppressWarnings("unused")
public class Team extends ManagedEntity implements IManagedEntity
{

    @Attribute
    @Identifier
    private String teamName;

    @Relationship(
            type = RelationshipType.ONE_TO_MANY,
            inverse = "team",
            inverseClass = Player.class,
            cascadePolicy = CascadePolicy.ALL,
            fetchPolicy = FetchPolicy.EAGER
    )
    private List<Player> players;

    @Relationship(
            type = RelationshipType.MANY_TO_ONE,
            inverse = "teams",
            inverseClass = Division.class
    )
    private Division division;

    public Team()
    {
    }

    public String getTeamName()
    {
        return teamName;
    }

    public void setTeamName(String teamName)
    {
        this.teamName = teamName;
    }

    public List<Player> getPlayers()
    {
        return players;
    }

    public void setPlayers(List<Player> players)
    {
        this.players = players;
    }

    public void setDivision(Division division)
    {
        this.division = division;
    }

    public Division getDivision()
    {
        return this.division;
    }
}
