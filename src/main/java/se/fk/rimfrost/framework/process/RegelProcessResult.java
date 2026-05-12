package se.fk.rimfrost.framework.process;

import se.fk.rimfrost.framework.regel.RegelErrorInformation;
import se.fk.rimfrost.framework.regel.Utfall;

public class RegelProcessResult {

    private final Utfall utfall;
    private final RegelErrorInformation error;

    public RegelProcessResult(Utfall utfall, RegelErrorInformation error) {
        this.utfall = utfall;
        this.error = error;
    }

    public Utfall getUtfall() {
        return utfall;
    }

    public RegelErrorInformation getError() {
        return error;
    }
}
