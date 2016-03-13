package com.nsysmon.data;

/**
 * @author arno
 */
public class ACorrelationId {
    private final String qualifier;
    private final String id;
    private final String idParent;

    /**
     * @param qualifier represents a 'kind' of correlation ID. This allows an application to differentiate between e.g.
     *                  SOA correlation IDs vs. correlation IDs of internal asynchronous cascades vs. 'conversation' IDs
     *                  in a web application
     * @param id is the actual identifier.
     * @param idParent is the identifier of the parent, used for hierachical ordering.
     */
    public ACorrelationId(String qualifier, String id, String idParent) {
        this.qualifier = qualifier;
        this.id = id;
        this.idParent = idParent;
    }

    public String getQualifier() {
        return qualifier;
    }

    public String getId() {
        return id;
    }

    public String getIdParent() {
        return idParent;
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ACorrelationId that = (ACorrelationId) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (idParent != null ? !idParent.equals(that.idParent) : that.idParent != null) return false;
        if (qualifier != null ? !qualifier.equals(that.qualifier) : that.qualifier != null) return false;

        return true;
    }

    @Override public int hashCode() {
        int result = qualifier != null ? qualifier.hashCode() : 0;
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (idParent != null ? idParent.hashCode() : 0);
        return result;
    }

    @Override public String toString() {
        return "ACorrelationId{" +
                "qualifier='" + qualifier + '\'' +
                ", id='" + id + '\'' +
                ", idParent='" + idParent + '\'' +
                "} " + super.toString();
    }
}
