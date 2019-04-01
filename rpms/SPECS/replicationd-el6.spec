%define __jar_repack {%nil}
# Compatibility with ncar
%define _source_payload w0.gzdio
%define _binary_payload w0.gzdio
%define _binary_filedigest_algorithm 1

# For use below
%define _prefix %{_usr}/local/chronopolis/replication
%define jar replicationd.jar
%define yaml application.yml
%define initsh /etc/init.d/replicationd
%define build_date %(date +"%Y%m%d")

Name: replicationd
Version: %{ver}
Release: %{build_date}.el6
Source: replication-shell.jar
Source1: replication.sh
Source2: replication-application.yml
Summary: Chronopolis Replication Service
License: BSD-3
URL: https://gitlab.umiacs.umd.edu/chronopolis
Group: System Environment/Daemons
autoprov: yes
autoreq: yes
BuildArch: noarch
BuildRoot: ${_tmppath}/build-%{name}-%{version}

%description
The Replication Services monitors for packages being ingested into Chronopolis
and does replication and registration on them.

%install

%__install -D -m0644 "%{SOURCE0}" "%{buildroot}%{_prefix}/%{jar}"
%__install -D -m0644 "%{SOURCE2}" "%{buildroot}%{_prefix}/%{yaml}"
%__install -D -m0755 "%{SOURCE1}" "%{buildroot}%{initsh}"

%files

%defattr(-,root,root)
%dir %{_prefix}
%{_prefix}/%{jar}
%config(noreplace) %{_prefix}/%{yaml}
%{initsh}

%post

chkconfig --add  replicationd

%preun

chkconfig --del replicationd

%changelog

* Tue Mar 5 2019 Mike Ritter <shake@umiacs.umd.edu> 3.1.0-20190305
- Set license to BSD 3 clause

* Fri Dec 1 2017 Mike Ritter <shake@umiacs.umd.edu> 2.0.5-20171201
- correct chkconfig service name

* Wed Nov 8 2017 Mike Ritter <shake@umiacs.umd.edu> 2.0.3-20171108
- remove install commands for logging directory

* Tue Oct 3 2017 Mike Ritter <shake@umiacs.umd.edu> 1.6.0-20171003
- cleanup spec to include missing sections
