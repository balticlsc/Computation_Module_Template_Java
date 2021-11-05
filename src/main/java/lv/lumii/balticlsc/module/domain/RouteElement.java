package lv.lumii.balticlsc.module.domain;

import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.variable.InverseRelationShadowVariable;

@PlanningEntity
public interface RouteElement {

    @InverseRelationShadowVariable(sourceVariableName = "previous")
    Customer getNextCustomer();

    void setNextCustomer(Customer nextCustomer);

    public Location getLocation();


}
