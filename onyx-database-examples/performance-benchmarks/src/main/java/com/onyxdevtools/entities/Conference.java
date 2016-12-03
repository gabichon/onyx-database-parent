package com.onyxdevtools.entities;

import com.onyx.persistence.IManagedEntity;
import com.onyx.persistence.ManagedEntity;
import com.onyx.persistence.annotations.Attribute;
import com.onyx.persistence.annotations.CascadePolicy;
import com.onyx.persistence.annotations.Entity;
import com.onyx.persistence.annotations.FetchPolicy;
import com.onyx.persistence.annotations.Identifier;
import com.onyx.persistence.annotations.Relationship;
import com.onyx.persistence.annotations.RelationshipType;

import javax.persistence.CascadeType;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author cosborn
 */
//J-
@Entity
@javax.persistence.Entity
public class Conference extends ManagedEntity implements IManagedEntity
{

    @Attribute
    @Identifier
    @Id
    public String conferenceName;

    @Relationship(
            type = RelationshipType.ONE_TO_MANY,
            inverseClass = Division.class,
            inverse = "conference",
            cascadePolicy = CascadePolicy.ALL,
            fetchPolicy = FetchPolicy.EAGER
    )
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, targetEntity = Division.class)
    protected List<Division> divisions = new ArrayList();

    @SuppressWarnings("unused")
    public String getConferenceName()
    {
        return conferenceName;
    }

    @SuppressWarnings("unused")
    public void setConferenceName(String conferenceName)
    {
        this.conferenceName = conferenceName;
    }

    @SuppressWarnings("unused")
    public List<Division> getDivisions()
    {
        return divisions;
    }

    @SuppressWarnings("unused")
    public void setDivisions(List<Division> divisions)
    {
        this.divisions = divisions;
    }

}