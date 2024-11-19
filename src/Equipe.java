
public 	class Equipe{
	Equipe( int _id, int _num, String _eq1, String _eq2){
		id = _id;
		num = _num;
		eq1 = _eq1;
		eq2 = _eq2;
	}
	public int id;
	public int num;
	public String eq1;
	public String eq2;

	public int getId() {
		return id;
	}

	public int getNum() {
		return num;
	}

	public String getEq1() {
		return eq1;
	}

	public String getEq2() {
		return eq2;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setNum(int num) {
		this.num = num;
	}

	public void setEq1(String eq1) {
		this.eq1 = eq1;
	}

	public void setEq2(String eq2) {
		this.eq2 = eq2;
	}
}
