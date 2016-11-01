package hbvOptimization;

public class Parameter {
	private String name;
	private double lowBound;
	private double upBound;
	private int index;
	private double value;
	
	public double getValue() {
		return value;
	}
	public void setValue(double value) {
		this.value = value;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public double getLowBound() {
		return lowBound;
	}
	public void setLowBound(double lowBound) {
		this.lowBound = lowBound;
	}
	public double getUpBound() {
		return upBound;
	}
	public void setUpBound(double upBound) {
		this.upBound = upBound;
	}
	public int getIndex() {
		return index;
	}
	public void setIndex(int index) {
		this.index = index;
	}
}
